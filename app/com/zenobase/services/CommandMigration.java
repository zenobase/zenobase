package com.zenobase.services;

import java.io.IOException;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
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
import com.zenobase.json.DomainNode;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
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
		try (RestClient client = RestClient.builder(HttpHost.create(sourceHost)).build()) {
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
							migrateEvents(client, owner, bucket);
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
		if (failures.get() > 0) {
			throw new IllegalStateException("Migration completed with one or more failures");
		}
	}

	private void migrateEvents(RestClient client, Identity owner, Bucket bucket) {
		List<Event> batch = new ArrayList<>();
		AtomicInteger batchNum = new AtomicInteger(1);
		scroll(client, bucket.getId(), 1000, source -> {
			batch.add(new Event(source));
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
					if (hits.size() == 0) {
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
					Request scrollRequest = new Request("POST", "/_search/scroll");
					scrollRequest.setEntity(new StringEntity(
						"{\"scroll\":\"5m\",\"scroll_id\":\"" + scrollId + "\"}",
						ContentType.APPLICATION_JSON));
					Response scrollResponse = client.performRequest(scrollRequest);
					response = (ObjectNode) Nodes.MAPPER.readTree(scrollResponse.getEntity().getContent());
					scrollId = response.get("_scroll_id").asText();
				}
			} finally {
				try {
					Request clearRequest = new Request("DELETE", "/_search/scroll");
					clearRequest.setEntity(new StringEntity(
						"{\"scroll_id\":\"" + scrollId + "\"}",
						ContentType.APPLICATION_JSON));
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
