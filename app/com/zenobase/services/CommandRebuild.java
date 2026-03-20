package com.zenobase.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

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
import play.Logger.ALogger;

import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.commands.CreateCredentialsCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.CreateTaskCommand;
import com.zenobase.commands.CreateUserCommand;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;

public class CommandRebuild {

	private final ALogger log = Logger.of("rebuild");
	private final String sourceHost;
	private final int parallelism;
	private final CommandDispatcher dispatcher;

	@Inject
	public CommandRebuild(@Named("opensearch.rebuild.host") String sourceHost, @Named("opensearch.rebuild.parallelism") int parallelism, CommandDispatcher dispatcher) {
		this.sourceHost = sourceHost;
		this.parallelism = parallelism;
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
		log.info("Rebuilding history from {}...", sourceHost);
		Stopwatch timer = Stopwatch.createStarted();
		rebuild("users", indexManager, this::rebuildUsers);
		rebuild("authorizations", indexManager, this::rebuildAuthorizations);
		rebuild("credentials", indexManager, this::rebuildCredentials);
		rebuild("buckets", indexManager, this::rebuildBuckets);
		rebuild("tasks", indexManager, this::rebuildTasks);
		log.warn("Rebuilt history in {} s", timer.elapsed(TimeUnit.SECONDS));
	}

	private void rebuild(String label, IndexManager indexManager, ToIntFunction<IndexManager> action) {
		Stopwatch timer = Stopwatch.createStarted();
		int count = action.applyAsInt(indexManager);
		log.info("Rebuilt {} {} in {} s", count, label, timer.elapsed(TimeUnit.SECONDS));
	}

	private int rebuildUsers(IndexManager indexManager) {
		AtomicInteger count = new AtomicInteger();
		new UserRepository(indexManager).findAll(user -> {
			dispatcher.dispatch(new CreateUserCommand(user.asIdentity(), user));
			count.incrementAndGet();
		});
		return count.get();
	}

	private int rebuildAuthorizations(IndexManager indexManager) {
		AtomicInteger count = new AtomicInteger();
		new AuthorizationRepository(indexManager).findAll(authorization -> {
			dispatcher.dispatch(new CreateAuthorizationCommand(authorization.getPrincipal(), authorization));
			count.incrementAndGet();
		});
		return count.get();
	}

	private int rebuildCredentials(IndexManager indexManager) {
		AtomicInteger count = new AtomicInteger();
		new CredentialsRepository(indexManager).findAll(credential -> {
			dispatcher.dispatch(new CreateCredentialsCommand(credential.getPrincipal(), credential));
			count.incrementAndGet();
		});
		return count.get();
	}

	private int rebuildBuckets(IndexManager indexManager) {
		EventRepository events = new EventRepository(indexManager);
		BucketRepository buckets = new BucketRepository(indexManager);
		List<Bucket> allBuckets = new ArrayList<>();
		buckets.findAll(allBuckets::add);
		runInParallel(
			allBuckets,
			bucket -> Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER)).getId(),
			bucket -> {
				Identity owner = Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER));
				if (!events.exists(bucket.getId())) {
					log.warn("Fixing missing aliases for bucket {}", bucket.getId());
					buckets.realias(bucket);
				}
				dispatcher.dispatch(new CreateBucketCommand(owner, bucket));
				if (!bucket.isVirtual()) {
					rebuildEvents(events, owner, bucket.getId(), bucket.getCreated());
				}
			},
			Bucket::getId
		);
		return allBuckets.size();
	}

	private <T> void runInParallel(List<T> items, Function<T, String> laneKey, Consumer<T> action, Function<T, String> itemLabel) {
		AtomicInteger failures = new AtomicInteger();
		int effectiveParallelism = Math.max(parallelism, 1);
		ExecutorService[] lanes = new ExecutorService[effectiveParallelism];
		for (int i = 0; i < effectiveParallelism; ++i) {
			lanes[i] = Executors.newSingleThreadExecutor();
		}
		Semaphore semaphore = new Semaphore(effectiveParallelism * 100);
		for (T item : items) {
			int lane = Math.abs(laneKey.apply(item).hashCode() % effectiveParallelism);
			try {
				semaphore.acquire();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			lanes[lane].submit(() -> {
				try {
					action.accept(item);
				} catch (RuntimeException e) {
					log.error("Couldn't rebuild: " + itemLabel.apply(item), e);
					failures.incrementAndGet();
				} finally {
					semaphore.release();
				}
			});
		}
		for (ExecutorService lane : lanes) {
			lane.shutdown();
		}
		for (ExecutorService lane : lanes) {
			try {
				lane.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
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

	private int rebuildTasks(IndexManager indexManager) {
		AtomicInteger count = new AtomicInteger();
		new TaskRepository(indexManager).findAll(task -> {
			task.setUndoId(null);
			dispatcher.dispatch(new CreateTaskCommand(task.getPrincipal(), task));
			count.incrementAndGet();
		});
		return count.get();
	}
}
