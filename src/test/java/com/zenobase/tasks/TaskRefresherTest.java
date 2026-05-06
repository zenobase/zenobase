package com.zenobase.tasks;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.services.CommandDispatcher;
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

		new TaskRefresher(registry, buckets, dispatcher).refresh(task);
	}

	@Test
	public void testBadBucket() {
		String taskType = "test";
		User user = new User("tester");
		Bucket bucket = new Bucket();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		Task task = new Task(taskType, bucket.getId(), user.asIdentity());

		new TaskRefresher(registry, buckets, dispatcher).refresh(task);

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

		new TaskRefresher(registry, buckets, dispatcher).refresh(task);

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

		new TaskRefresher(registry, buckets, dispatcher).refresh(task);

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
			new TaskRefresher(registry, buckets, dispatcher).refresh(task)
		);
		assert thrown == incomplete;
		verify(dispatcher).dispatch(recoveryCommand);
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

		new TaskRefresher(registry, buckets, dispatcher).refresh(task);
		verifyNoInteractions(dispatcher);
	}
}
