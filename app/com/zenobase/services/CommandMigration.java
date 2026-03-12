package com.zenobase.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import play.Logger;

import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.commands.CreateCredentialsCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.CreateTaskCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.common.Callback;
import com.zenobase.json.DecimalField;
import com.zenobase.json.PercentageField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IntegerField;
import com.zenobase.json.LocationField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.json.Field;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.Task;

public class CommandMigration {

	private final String sourceHost;
	private final int parallelism;
	private final CommandDispatcher dispatcher;

	@Inject
	public CommandMigration(@Named("es.migration.host") String sourceHost, @Named("es.migration.parallelism") int parallelism, CommandDispatcher dispatcher) {
		this.sourceHost = sourceHost;
		this.parallelism = parallelism > 0 ? parallelism : Runtime.getRuntime().availableProcessors();
		this.dispatcher = dispatcher;
	}

	public void migrate() {
		Logger.info("Migrating from {}...", sourceHost);
		Stopwatch timer = Stopwatch.createStarted();
		try (RestClient client = RestClient.builder(HttpHost.create(java.net.URI.create(sourceHost))).build()) {
			migrateUsers(client);
			migrateAuthorizations(client);
			migrateCredentials(client);
			migrateBuckets(client);
			migrateTasks(client);
		} catch (IOException e) {
			throw new RuntimeException("Migration failed", e);
		}
		Logger.warn("Migrated in {} s", timer.elapsed(TimeUnit.SECONDS));
	}

	private void migrateUsers(RestClient client) {
		scroll(client, "users", 100, source -> {
			User user = new User(source);
			dispatcher.dispatch(new CreateUserCommand(user.asIdentity(), user));
		});
	}

	private void migrateAuthorizations(RestClient client) {
		scroll(client, "authorizations", 100, source -> {
			Authorization authorization = new Authorization(source);
			dispatcher.dispatch(new CreateAuthorizationCommand(authorization.getPrincipal(), authorization));
		});
	}

	private void migrateCredentials(RestClient client) {
		scroll(client, "credentials", 100, source -> {
			Credentials credential = new Credentials(source);
			dispatcher.dispatch(new CreateCredentialsCommand(credential.getPrincipal(), credential));
		});
	}

	private void migrateBuckets(RestClient client) {
		AtomicInteger failures = new AtomicInteger();
		AtomicInteger repairs = new AtomicInteger();
		ExecutorService[] lanes = new ExecutorService[parallelism];
		for (int i = 0; i < parallelism; ++i) {
			lanes[i] = Executors.newSingleThreadExecutor();
		}
		Semaphore semaphore = new Semaphore(parallelism * 100);
		try {
			scroll(client, "buckets", 100, source -> {
				Bucket bucket = new Bucket(source);
				Identity owner = Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER));
				int lane = Math.floorMod(owner.getId().hashCode(), parallelism);
				semaphore.acquireUninterruptibly();
				lanes[lane].submit(() -> {
					try {
						dispatcher.dispatch(new CreateBucketCommand(owner, bucket));
						if (!bucket.isVirtual()) {
							migrateEvents(client, owner, bucket, failures, repairs);
						}
					} catch (RuntimeException e) {
						Logger.error("Couldn't migrate bucket: " + bucket.getId(), e);
						failures.incrementAndGet();
					} finally {
						semaphore.release();
					}
				});
			});
			for (ExecutorService lane : lanes) {
				lane.shutdown();
			}
			for (ExecutorService lane : lanes) {
				lane.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Migration interrupted", e);
		} finally {
			for (ExecutorService lane : lanes) {
				lane.shutdownNow();
			}
		}
		if (repairs.get() > 0) {
			Logger.warn("Migration repaired {} field value(s)", repairs.get());
		}
		if (failures.get() > 0) {
			throw new IllegalStateException("Migration completed with one or more failures");
		}
	}

	private void migrateEvents(RestClient client, Identity owner, Bucket bucket, AtomicInteger failures, AtomicInteger repairs) {
		List<Event> batch = new ArrayList<>();
		AtomicInteger batchNum = new AtomicInteger(1);
		scroll(client, bucket.getId(), 1000, source -> {
			Event event = new Event(source);
			failures.addAndGet(validateEvent(event, source, bucket.getId(), repairs));
			batch.add(event);
			if (batch.size() == 1000) {
				dispatcher.dispatch(new CreateEventsCommand(owner, bucket.getId(), batch, bucket.getCreated().plusMillis(batchNum.get())));
				batch.clear();
				batchNum.incrementAndGet();
			}
		});
		if (!batch.isEmpty()) {
			dispatcher.dispatch(new CreateEventsCommand(owner, bucket.getId(), batch, bucket.getCreated().plusMillis(batchNum.get())));
		}
	}

	private JsonNode repairElement(Field<?> field, JsonNode element) {
		if (field == Event.RATING && element.isNumber()) {
			int clamped = Math.max(Rating.MIN_VALUE, Math.min(Rating.MAX_VALUE, element.intValue()));
			if (clamped != element.intValue()) return IntNode.valueOf(clamped);
		}
		if (field instanceof PercentageField && element.isNumber()) {
			BigDecimal value = element.decimalValue();
			BigDecimal clamped = value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
			if (clamped.compareTo(value) != 0) return DecimalNode.valueOf(clamped);
		}
		if (field instanceof IntegerField && element.isTextual()) {
			try { return IntNode.valueOf(Integer.parseInt(element.textValue())); }
			catch (NumberFormatException e) { return null; }
		}
		if (field instanceof DecimalField && element.isTextual()) {
			try { return DecimalNode.valueOf(BigDecimal.valueOf(Double.parseDouble(element.textValue()))); }
			catch (NumberFormatException e) { return null; }
		}
		if (field == Event.TIMESTAMP && element.isTextual()) {
			String upper = element.textValue().toUpperCase();
			if (!upper.equals(element.textValue())) return new TextNode(upper);
		}
		if (field == Event.RESOURCE && element.isObject()) {
			if (repairResource((ObjectNode) element)) return element;
		}
		if (field instanceof LocationField && element.isObject()) {
			if (repairLocation((ObjectNode) element)) return element;
		}
		return null;
	}

	private boolean repairField(Field<?> field, ObjectNode source, String eventId, String bucketId) {
		JsonNode rawValue = source.get(field.getName());
		if (rawValue == null) return false;

		if (rawValue.isArray()) {
			ArrayNode array = (ArrayNode) rawValue;
			boolean anyRepaired = false;
			for (int i = 0; i < array.size(); i++) {
				JsonNode repaired = repairElement(field, array.get(i));
				if (repaired != null) {
					Logger.warn("Repaired " + field.getName() + "[" + i + "] in event " + eventId
						+ " of bucket " + bucketId + ": " + array.get(i) + " -> " + repaired);
					array.set(i, repaired);
					anyRepaired = true;
				}
			}
			for (int i = array.size() - 1; i >= 0; i--) {
				if (array.get(i).isNull()) {
					array.remove(i);
					anyRepaired = true;
				}
			}
			if (anyRepaired) {
				Logger.warn("Repaired " + field.getName() + " in event " + eventId
					+ " of bucket " + bucketId);
				return true;
			}
		} else {
			JsonNode repaired = repairElement(field, rawValue);
			if (repaired != null) {
				Logger.warn("Repaired " + field.getName() + " in event " + eventId
					+ " of bucket " + bucketId + ": " + rawValue + " -> " + repaired);
				source.set(field.getName(), repaired);
				return true;
			}
		}
		return false;
	}

	private boolean repairResource(ObjectNode resource) {
		boolean repaired = false;
		if (resource.get("url") == null) {
			resource.put("url", "");
			repaired = true;
		}
		if (resource.get("title") == null) {
			if ("http://www.moves-app.com/".equals(resource.path("url").textValue())) {
				resource.put("title", "Moves");
				repaired = true;
			}
		}
		return repaired;
	}

	private boolean repairLocation(ObjectNode location) {
		boolean repaired = false;
		Iterator<String> fieldNames = location.fieldNames();
		while (fieldNames.hasNext()) {
			String name = fieldNames.next();
			if (!"lat".equals(name) && !"lon".equals(name)) {
				fieldNames.remove();
				repaired = true;
			}
		}
		repaired |= repairCoordinate(location, "lat");
		repaired |= repairCoordinate(location, "lon");
		if (location.get("lat") == null || location.get("lon") == null) {
			return false;
		}
		if (!new Location(location.get("lat").decimalValue(), location.get("lon").decimalValue()).isValid()) {
			return false;
		}
		return repaired;
	}

	private boolean repairCoordinate(ObjectNode location, String name) {
		JsonNode value = location.get(name);
		if (value == null) return false;
		if (value.isNull()) {
			location.remove(name);
			return false;
		}
		if (value.isTextual()) {
			try {
				location.put(name, Double.parseDouble(value.textValue()));
				return true;
			} catch (NumberFormatException e) {
				location.remove(name);
				return false;
			}
		}
		return false;
	}

	private int validateEvent(Event event, ObjectNode source, String bucketId, AtomicInteger repairs) {
		int failures = 0;
		for (Field<?> field : Event.FIELDS) {
			if (event.contains(field)) {
				try {
					field.getValues(source);
				} catch (Exception e) {
					if (repairField(field, source, event.getId(), bucketId)) {
						repairs.incrementAndGet();
					} else {
						Logger.error("Malformed " + field.getName() + " in event " + event.getId()
							+ " of bucket " + bucketId + ": value=" + source.get(field.getName())
							+ ", error=" + e.getMessage());
						source.remove(field.getName());
						failures++;
					}
				}
			}
		}
		return failures;
	}

	private void migrateTasks(RestClient client) {
		scroll(client, "tasks", 100, source -> {
			Task task = new Task(source);
			task.setUndoId(null);
			dispatcher.dispatch(new CreateTaskCommand(task.getPrincipal(), task));
		});
	}

	private void scroll(RestClient client, String index, int size, Callback<ObjectNode> callback) {
		try {
			Request initRequest = new Request("POST", "/" + index + "/_search?scroll=5m");
			initRequest.setEntity(new StringEntity(
				"{\"size\":" + size + ",\"query\":{\"match_all\":{}}}",
				ContentType.APPLICATION_JSON));
			Response initResponse = client.performRequest(initRequest);
			ObjectNode response = (ObjectNode) Nodes.MAPPER.readTree(initResponse.getEntity().getContent());

			String scrollId = response.get("_scroll_id").asText();
			try {
				while (true) {
					JsonNode hits = response.get("hits").get("hits");
					if (hits.isEmpty()) {
						break;
					}
					for (JsonNode hit : hits) {
						ObjectNode source = (ObjectNode) hit.get("_source");
						long version = hit.has("_version") ? hit.get("_version").asLong() : 0;
						if (version > 0) {
							DomainNode.VERSION.setValue(source, version);
						}
						callback.call(source);
					}
					Request scrollRequest = new Request("POST", "/_search/scroll?scroll=5m&scroll_id=" + scrollId);
					Response scrollResponse = client.performRequest(scrollRequest);
					response = (ObjectNode) Nodes.MAPPER.readTree(scrollResponse.getEntity().getContent());
					scrollId = response.get("_scroll_id").asText();
				}
			} finally {
				try {
					Request clearRequest = new Request("DELETE", "/_search/scroll?scroll_id=" + scrollId);
					client.performRequest(clearRequest);
				} catch (IOException e) {
					Logger.warn("Failed to clear scroll", e);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to scroll index: " + index, e);
		}
	}
}
