package com.zenobase.tasks;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

public class TaskRefresherTest {

	private final TaskManagerRegistry registry = mock(TaskManagerRegistry.class);
	private final TaskManager manager = mock(TaskManager.class);
	private final BucketRepository buckets = mock(BucketRepository.class);
	private final CommandDispatcher dispatcher = mock(CommandDispatcher.class);

	@Test
	public void test() {
		String taskType = "test";
		User user = new User("tester");
		Bucket bucket = new Bucket();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		Task task = new Task(taskType, bucket.getId(), user.asIdentity());
		String commandId = Generator.id();
		Command command = mock(Command.class);

		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(registry.exists(taskType)).thenReturn(true);
		when(registry.find(taskType)).thenReturn(manager);
		when(manager.execute(task)).thenReturn(command);
		when(dispatcher.dispatch(ArgumentMatchers.any(Command.class))).thenReturn(commandId);

		new TaskRefresher(registry, buckets, dispatcher, new LocalBus()).refresh(task);
	}

	@Test
	public void testBadBucket() {
		String taskType = "test";
		User user = new User("tester");
		Bucket bucket = new Bucket();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		Task task = new Task(taskType, bucket.getId(), user.asIdentity());

		new TaskRefresher(registry, buckets, dispatcher, new LocalBus()).refresh(task);

		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testBadRole() {
		String taskType = "test";
		User user = new User("tester");
		Bucket bucket = new Bucket();
		bucket.addRole(user.asIdentity(), Role.VIEWER);
		Task task = new Task(taskType, bucket.getId(), user.asIdentity());

		when(buckets.find(bucket.getId())).thenReturn(bucket);

		new TaskRefresher(registry, buckets, dispatcher, new LocalBus()).refresh(task);

		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testBadType() {
		String taskType = "test";
		User user = new User("tester");
		Bucket bucket = new Bucket();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		Task task = new Task(taskType, bucket.getId(), user.asIdentity());

		when(buckets.find(bucket.getId())).thenReturn(bucket);

		new TaskRefresher(registry, buckets, dispatcher, new LocalBus()).refresh(task);

		verifyNoInteractions(dispatcher);
	}

	@Test
	public void testInvalidTokenRecovery() {
		String taskType = "test";
		User user = new User("tester");
		Bucket bucket = new Bucket();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		Task task = new Task(taskType, bucket.getId(), user.asIdentity());
		Command recoveryCommand = mock(Command.class);
		OAuthTaskManager oauthManager = mock(OAuthTaskManager.class);
		OAuthCredentials credentials = mock(OAuthCredentials.class);
		InvalidTokenException invalid = new InvalidTokenException(credentials);
		IncompleteCredentialsException incomplete = new IncompleteCredentialsException(credentials);

		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(registry.exists(taskType)).thenReturn(true);
		when(registry.find(taskType)).thenReturn(oauthManager);
		when(oauthManager.execute(task)).thenThrow(invalid);
		when(oauthManager.recoverInvalidToken(invalid)).thenReturn(recoveryCommand);
		when(oauthManager.reload(invalid)).thenThrow(incomplete);

		IncompleteCredentialsException thrown = assertThrows(IncompleteCredentialsException.class, () ->
			new TaskRefresher(registry, buckets, dispatcher, new LocalBus()).refresh(task)
		);
		assert thrown == incomplete;
		verify(dispatcher).dispatch(recoveryCommand);
	}

	@Test
	public void testConcurrentRefreshIsSkipped() throws Exception {
		String taskType = "test";
		User user = new User("tester");
		Bucket bucket = new Bucket();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		Task task = new Task(taskType, bucket.getId(), user.asIdentity());
		Command command = mock(Command.class);

		CountDownLatch firstInside = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);

		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(registry.exists(taskType)).thenReturn(true);
		when(registry.find(taskType)).thenReturn(manager);
		when(manager.execute(task))
			.thenAnswer(invocation -> {
				firstInside.countDown();
				release.await();
				return command;
			})
			.thenReturn(command);
		when(dispatcher.dispatch(ArgumentMatchers.any(Command.class))).thenReturn(Generator.id());

		TaskRefresher refresher = new TaskRefresher(registry, buckets, dispatcher, new LocalBus());

		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			var first = pool.submit(() -> refresher.refresh(task));
			assertTrue(firstInside.await(2, TimeUnit.SECONDS), "first refresh should reach manager.execute");
			// Second refresh runs while the first is blocked; should short-circuit and return.
			pool.submit(() -> refresher.refresh(task)).get(2, TimeUnit.SECONDS);
			release.countDown();
			first.get(2, TimeUnit.SECONDS);
		} finally {
			release.countDown();
			pool.shutdown();
			assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
		}

		// Only the first refresh dispatched; the concurrent one was skipped.
		verify(dispatcher, times(1)).dispatch(command);
		verify(manager, times(1)).execute(task);

		// After the first refresh completes, a new refresh for the same task is allowed again.
		refresher.refresh(task);
		verify(dispatcher, times(2)).dispatch(command);
		verify(manager, times(2)).execute(task);
	}

	@Test
	public void testNoop() {
		String taskType = "test";
		User user = new User("tester");
		Bucket bucket = new Bucket();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		Task task = new Task(taskType, bucket.getId(), user.asIdentity());

		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(registry.exists(taskType)).thenReturn(true);
		when(registry.find(taskType)).thenReturn(manager);

		new TaskRefresher(registry, buckets, dispatcher, new LocalBus()).refresh(task);
		verifyNoInteractions(dispatcher);
	}
}
