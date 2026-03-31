package com.zenobase.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.Task;

public class CommandRebuild {

	private static final Logger logger = LoggerFactory.getLogger(CommandRebuild.class);

	private final String sourceHost;
	private final int parallelism;
	private final CommandDispatcher dispatcher;
	private final UserRepository targetUsers;
	private final AuthorizationRepository targetAuthorizations;
	private final CredentialsRepository targetCredentials;
	private final BucketRepository targetBuckets;
	private final TaskRepository targetTasks;

	@Inject
	public CommandRebuild(
			@Named("opensearch.rebuild") String sourceHost,
			@Named("opensearch.rebuild_parallelism") int parallelism,
			CommandDispatcher dispatcher,
			UserRepository targetUsers,
			AuthorizationRepository targetAuthorizations,
			CredentialsRepository targetCredentials,
			BucketRepository targetBuckets,
			TaskRepository targetTasks) {
		this.sourceHost = sourceHost;
		this.parallelism = parallelism;
		this.dispatcher = dispatcher;
		this.targetUsers = targetUsers;
		this.targetAuthorizations = targetAuthorizations;
		this.targetCredentials = targetCredentials;
		this.targetBuckets = targetBuckets;
		this.targetTasks = targetTasks;
	}

	public void rebuild() {
		if (!sourceHost.isEmpty()) {
			ClientFactory factory = () -> OpenSearchClientFactory.createHttpClient(sourceHost);
			IndexManager indexManager = new IndexManager(factory);
			rebuild(indexManager);
			indexManager.close();
		}
	}

	void rebuild(IndexManager indexManager) {
		logger.info("Rebuilding history from {}...", sourceHost);
		Stopwatch timer = Stopwatch.createStarted();
		rebuild(indexManager, targetUsers, "users", this::rebuildUsers);
		rebuild(indexManager, targetAuthorizations, "authorizations", this::rebuildAuthorizations);
		rebuild(indexManager, targetCredentials, "credentials", this::rebuildCredentials);
		rebuild(indexManager, targetBuckets, "buckets", this::rebuildBuckets);
		rebuild(indexManager, targetTasks, "tasks", this::rebuildTasks);
		logger.warn("Rebuilt history in {} s", timer.elapsed(TimeUnit.SECONDS));
	}

	private void rebuild(
			IndexManager indexManager,
			RepositorySupport<?> targetRepo,
			String label,
			ToIntFunction<IndexManager> action) {
		targetRepo.disableRefresh(true);
		try {
			Stopwatch timer = Stopwatch.createStarted();
			int count = action.applyAsInt(indexManager);
			logger.info("Rebuilt {} {} in {} s", count, label, timer.elapsed(TimeUnit.SECONDS));
		} finally {
			targetRepo.refresh();
			targetRepo.disableRefresh(false);
		}
	}

	private int rebuildUsers(IndexManager indexManager) {
		List<User> allUsers = new ArrayList<>();
		new UserRepository(indexManager).findAll(allUsers::add);
		allUsers.forEach(user -> dispatcher.dispatch(new CreateUserCommand(user.asIdentity(), user)));
		return allUsers.size();
	}

	private int rebuildAuthorizations(IndexManager indexManager) {
		List<Authorization> allAuthorizations = new ArrayList<>();
		new AuthorizationRepository(indexManager).findAll(allAuthorizations::add);
		allAuthorizations.forEach(authorization ->
				dispatcher.dispatch(new CreateAuthorizationCommand(authorization.getPrincipal(), authorization)));
		return allAuthorizations.size();
	}

	private int rebuildCredentials(IndexManager indexManager) {
		List<Credentials> allCredentials = new ArrayList<>();
		new CredentialsRepository(indexManager).findAll(allCredentials::add);
		allCredentials.forEach(
				credential -> dispatcher.dispatch(new CreateCredentialsCommand(credential.getPrincipal(), credential)));
		return allCredentials.size();
	}

	private int rebuildBuckets(IndexManager indexManager) {
		var events = new EventRepository(indexManager);
		var buckets = new BucketRepository(indexManager);
		List<Bucket> allBuckets = new ArrayList<>();
		buckets.findAll(allBuckets::add);
		runInParallel(
				allBuckets,
				bucket -> Objects.requireNonNull(Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER)))
						.id(),
				bucket -> {
					Identity owner = Objects.requireNonNull(Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER)));
					dispatcher.dispatch(new CreateBucketCommand(owner, bucket));
					if (!bucket.isVirtual()) {
						rebuildEvents(events, owner, bucket.getId(), Objects.requireNonNull(bucket.getCreated()));
					}
				},
				Bucket::getId);
		return allBuckets.size();
	}

	private <T> void runInParallel(
			List<T> items, Function<T, String> laneKey, Consumer<T> action, Function<T, String> itemLabel) {
		var failures = new AtomicInteger();
		int effectiveParallelism =
				parallelism > 0 ? parallelism : Math.max(2, Runtime.getRuntime().availableProcessors());
		logger.info("Using {} executor(s)", effectiveParallelism);
		ThreadPoolExecutor[] lanes = new ThreadPoolExecutor[effectiveParallelism];
		for (int i = 0; i < effectiveParallelism; ++i) {
			lanes[i] = new ThreadPoolExecutor(
					1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(100), (r, executor) -> {
						try {
							executor.getQueue().put(r);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new RuntimeException(e);
						}
					});
		}
		for (T item : items) {
			int lane = Math.abs(laneKey.apply(item).hashCode() % effectiveParallelism);
			lanes[lane].execute(() -> {
				try {
					action.accept(item);
				} catch (RuntimeException e) {
					logger.error("Couldn't rebuild: {}", itemLabel.apply(item), e);
					failures.incrementAndGet();
				}
			});
		}
		for (ThreadPoolExecutor lane : lanes) {
			lane.shutdown();
		}
		for (ThreadPoolExecutor lane : lanes) {
			try {
				if (!lane.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
					logger.warn("Lane did not terminate within the timeout");
				}
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
		var batchNum = new AtomicInteger(1);
		events.findAll(bucketId, event -> {
			batch.add(event);
			if (batch.size() == 5000) {
				dispatcher.dispatch(
						new CreateEventsCommand(owner, bucketId, batch, timestamp.plusMillis(batchNum.get())));
				batch.clear();
				batchNum.incrementAndGet();
			}
		});
		if (!batch.isEmpty()) {
			dispatcher.dispatch(new CreateEventsCommand(owner, bucketId, batch, timestamp.plusMillis(batchNum.get())));
		}
	}

	private int rebuildTasks(IndexManager indexManager) {
		List<Task> allTasks = new ArrayList<>();
		new TaskRepository(indexManager).findAll(allTasks::add);
		allTasks.forEach(task -> {
			task.setUndoId(null);
			dispatcher.dispatch(new CreateTaskCommand(task.getPrincipal(), task));
		});
		return allTasks.size();
	}
}
