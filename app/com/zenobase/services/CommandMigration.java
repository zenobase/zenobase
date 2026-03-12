package com.zenobase.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import com.zenobase.json.TokenField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.json.Field;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
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

	private boolean repairField(Field<?> field, ObjectNode source, String eventId, String bucketId) {
		JsonNode rawValue = source.get(field.getName());
		if (field == Event.RATING && rawValue != null && rawValue.isNumber()) {
			int clamped = Math.max(Rating.MIN_VALUE, Math.min(Rating.MAX_VALUE, rawValue.intValue()));
			Logger.warn("Repaired " + field.getName() + " in event " + eventId
				+ " of bucket " + bucketId + ": " + rawValue + " -> " + clamped);
			source.put(field.getName(), clamped);
			return true;
		}
		if (field instanceof PercentageField && rawValue != null && rawValue.isNumber()) {
			BigDecimal value = rawValue.decimalValue();
			BigDecimal clamped = value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
			if (clamped.compareTo(value) != 0) {
				Logger.warn("Repaired " + field.getName() + " in event " + eventId
					+ " of bucket " + bucketId + ": " + value + " -> " + clamped);
				source.put(field.getName(), clamped);
				return true;
			}
		}
		if (field instanceof IntegerField && rawValue != null && rawValue.isTextual()) {
			try {
				int repaired = Integer.parseInt(rawValue.textValue());
				Logger.warn("Repaired " + field.getName() + " in event " + eventId
					+ " of bucket " + bucketId + ": \"" + rawValue.textValue() + "\" -> " + repaired);
				source.put(field.getName(), repaired);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		if (field instanceof DecimalField && rawValue != null && rawValue.isTextual()) {
			try {
				double repaired = Double.parseDouble(rawValue.textValue());
				Logger.warn("Repaired " + field.getName() + " in event " + eventId
					+ " of bucket " + bucketId + ": \"" + rawValue.textValue() + "\" -> " + repaired);
				source.put(field.getName(), repaired);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		if (field instanceof TokenField && rawValue != null && rawValue.isArray()) {
			ArrayNode array = (ArrayNode) rawValue;
			boolean anyRepaired = false;
			for (int i = array.size() - 1; i >= 0; i--) {
				if (array.get(i).isNull()) {
					array.remove(i);
					anyRepaired = true;
				}
			}
			if (anyRepaired) {
				Logger.warn("Repaired " + field.getName() + " in event " + eventId
					+ " of bucket " + bucketId + ": removed null values");
				return true;
			}
		}
		if (field == Event.RESOURCE && rawValue != null) {
			boolean anyRepaired = false;
			if (rawValue.isObject()) {
				anyRepaired = repairResource((ObjectNode) rawValue);
			} else if (rawValue.isArray()) {
				for (JsonNode element : rawValue) {
					if (element.isObject()) {
						anyRepaired |= repairResource((ObjectNode) element);
					}
				}
			}
			if (anyRepaired) {
				Logger.warn("Repaired " + field.getName() + " in event " + eventId
					+ " of bucket " + bucketId + ": added missing title/url");
				return true;
			}
		}
		if (field == Event.TIMESTAMP && rawValue != null) {
			if (rawValue.isTextual()) {
				String repaired = rawValue.textValue().toUpperCase();
				if (!repaired.equals(rawValue.textValue())) {
					Logger.warn("Repaired " + field.getName() + " in event " + eventId
						+ " of bucket " + bucketId + ": " + rawValue.textValue() + " -> " + repaired);
					source.put(field.getName(), repaired);
					return true;
				}
			} else if (rawValue.isArray()) {
				boolean anyRepaired = false;
				ArrayNode array = (ArrayNode) rawValue;
				for (int i = 0; i < array.size(); i++) {
					if (array.get(i).isTextual()) {
						String repaired = array.get(i).textValue().toUpperCase();
						if (!repaired.equals(array.get(i).textValue())) {
							Logger.warn("Repaired " + field.getName() + "[" + i + "] in event " + eventId
								+ " of bucket " + bucketId + ": " + array.get(i).textValue() + " -> " + repaired);
							array.set(i, new TextNode(repaired));
							anyRepaired = true;
						}
					}
				}
				return anyRepaired;
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
