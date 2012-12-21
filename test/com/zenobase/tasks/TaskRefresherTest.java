package com.zenobase.tasks;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Matchers;

import com.zenobase.commands.Command;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;

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
		when(registry.find(taskType)).thenReturn(manager);
		when(manager.execute(task)).thenReturn(command);
		when(dispatcher.dispatch(Matchers.any(Command.class))).thenReturn(commandId);

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

		assertThat(task.getStatus()).isEqualTo(Task.Status.FAILED);
		verifyZeroInteractions(dispatcher);
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

		assertThat(task.getStatus()).isEqualTo(Task.Status.FAILED);
		verifyZeroInteractions(dispatcher);
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

		assertThat(task.getStatus()).isEqualTo(Task.Status.FAILED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testNoop() {

		String taskType = "test";
		User user = new User("tester");
		Bucket bucket = new Bucket();
		bucket.addRole(user.asIdentity(), Role.OWNER);
		Task task = new Task(taskType, bucket.getId(), user.asIdentity());

		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(registry.find(taskType)).thenReturn(manager);

		new TaskRefresher(registry, buckets, dispatcher).refresh(task);
		verifyZeroInteractions(dispatcher);
	}
}
