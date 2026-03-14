package com.zenobase.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.commands.CreateCredentialsCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.CreateTaskCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;

public class CommandRebuild {

	private final String sourceHost;
	private final int parallelism;
	private final CommandDispatcher dispatcher;

	@Inject
	public CommandRebuild(@Named("opensearch.rebuild.host") String sourceHost, @Named("opensearch.rebuild.parallelism") int parallelism, CommandDispatcher dispatcher) {
		this.sourceHost = sourceHost;
		this.parallelism = parallelism > 0 ? parallelism : Runtime.getRuntime().availableProcessors();
		this.dispatcher = dispatcher;
	}

	public void rebuild() {
		if (!sourceHost.isEmpty()) {
			ClientFactory factory = () -> {
				HttpHost httpHost = HttpHost.create(java.net.URI.create(sourceHost));
				return new OpenSearchClient(ApacheHttpClient5TransportBuilder
					.builder(httpHost)
					.setMapper(new JacksonJsonpMapper())
					.build());
			};
			IndexManager indexManager = new IndexManager(factory);
			rebuild(indexManager);
			indexManager.close();
		}
	}

	void rebuild(IndexManager indexManager) {
		Logger.info("Rebuilding history from {}...", sourceHost);
		Stopwatch timer = Stopwatch.createStarted();
		rebuildUsers(indexManager);
		rebuildAuthorizations(indexManager);
		rebuildCredentials(indexManager);
		rebuildBuckets(indexManager);
		rebuildTasks(indexManager);
		Logger.warn("Rebuilt history in {} s", timer.elapsed(TimeUnit.SECONDS));
	}

	private void rebuildUsers(IndexManager indexManager) {
		new UserRepository(indexManager).findAll(user ->
			dispatcher.dispatch(new CreateUserCommand(user.asIdentity(), user)));
	}

	private void rebuildAuthorizations(IndexManager indexManager) {
		new AuthorizationRepository(indexManager).findAll(authorization ->
			dispatcher.dispatch(new CreateAuthorizationCommand(authorization.getPrincipal(), authorization)));
	}

	private void rebuildCredentials(IndexManager indexManager) {
		new CredentialsRepository(indexManager).findAll(credential ->
			dispatcher.dispatch(new CreateCredentialsCommand(credential.getPrincipal(), credential)));
	}

	private void rebuildBuckets(IndexManager indexManager) {
		EventRepository events = new EventRepository(indexManager);
		BucketRepository buckets = new BucketRepository(indexManager);
		AtomicInteger failures = new AtomicInteger();
		ExecutorService[] lanes = new ExecutorService[parallelism];
		for (int i = 0; i < parallelism; ++i) {
			lanes[i] = Executors.newSingleThreadExecutor();
		}
		Semaphore semaphore = new Semaphore(parallelism * 100);
		try {
			buckets.findAll(bucket -> {
				if (!events.exists(bucket.getId())) {
					Logger.warn("Fixing missing aliases for bucket {}", bucket.getId());
					buckets.realias(bucket);
				}
				Identity owner = Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER));
				int lane = Math.floorMod(owner.getId().hashCode(), parallelism);
				semaphore.acquireUninterruptibly();
				lanes[lane].submit(() -> {
					try {
						dispatcher.dispatch(new CreateBucketCommand(owner, bucket));
						if (!bucket.isVirtual()) {
							rebuildEvents(events, owner, bucket.getId(), bucket.getCreated());
						}
					} catch (RuntimeException e) {
						Logger.error("Couldn't rebuild bucket: " + bucket.getId(), e);
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
			throw new RuntimeException("Rebuild interrupted", e);
		} finally {
			for (ExecutorService lane : lanes) {
				lane.shutdownNow();
			}
		}
		if (failures.get() > 0) {
			throw new IllegalStateException("Rebuild completed with one or more failures");
		}
	}

	private void rebuildEvents(EventRepository events, Identity owner, String bucketId, DateTime timestamp) {
		List<Event> batch = new ArrayList<>();
		AtomicInteger batchNum = new AtomicInteger(1);
		events.findAll(bucketId, event -> {
			batch.add(event);
			if (batch.size() == 1000) {
				dispatcher.dispatch(new CreateEventsCommand(owner, bucketId, batch, timestamp.plusMillis(batchNum.get())));
				batch.clear();
				batchNum.incrementAndGet();
			}
		});
		if (!batch.isEmpty()) {
			dispatcher.dispatch(new CreateEventsCommand(owner, bucketId, batch, timestamp.plusMillis(batchNum.get())));
		}
	}

	private void rebuildTasks(IndexManager indexManager) {
		new TaskRepository(indexManager).findAll(task -> {
			task.setUndoId(null);
			dispatcher.dispatch(new CreateTaskCommand(task.getPrincipal(), task));
		});
	}
}
