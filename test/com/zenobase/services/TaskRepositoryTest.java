package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class TaskRepositoryTest extends ElasticSearchTestSupport {

	private final String type = "test";
	private final String bucketId = Generator.id();
	private final Identity principal = new Identity();
	private TaskRepository repository;

	@Before
	public void setUp() {
		repository = new TaskRepository(getManager());
	}

	@Test
	public void testCRUD() {
		Task task = new Task(type, bucketId, principal);
		assertThat(repository.find(task.getId())).isNull();
		assertThat(repository.delete(task.getId())).isFalse();
		repository.store(task, DateTime.now());
		assertThat(repository.find(task.getId()).toJson()).isEqualTo(task.toJson());
		task.setCompleted(DateTime.now());
		repository.update(task, DateTime.now());
		assertThat(repository.find(task.getId()).toJson()).isEqualTo(task.toJson());
		assertThat(repository.delete(task.getId())).isTrue();
		assertThat(repository.find(task.getId())).isNull();
	}

	@Test
	public void testFindAll() {
		List<Task> tasks = fill(20, new Identity());
		assertThat(repository.find(0, 10)).hasTotal(tasks.size()).isEqualTo(tasks.subList(0, 10));
		assertThat(repository.find(10, 10)).hasTotal(tasks.size()).isEqualTo(tasks.subList(10, 20));
		assertThat(repository.find(20, 10)).hasTotal(tasks.size()).isEqualTo(Collections.emptyList());
	}

	@Test
	public void testFindByBucket() {
		Task expected = new Task(type, bucketId, principal);
		store(new Task(type, Generator.id(), principal));
		assertThat(repository.find(Task.BUCKET.getName(), expected.getBucketId(), 0, 10)).hasTotal(0);
		store(expected);
		assertThat(repository.find(Task.BUCKET.getName(), expected.getBucketId(), 0, 10)).hasTotal(1);
	}

	private List<Task> fill(int size, Identity principal) {
		List<Task> tasks = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; ++i) {
			Task task = new Task(type, bucketId, principal);
			tasks.add(task);
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS); // tasks will be returned in order of creation time
			repository.store(task, DateTime.now());
		}
		repository.refresh();
		return Lists.reverse(tasks);
	}

	private void store(Task task) {
		repository.store(task, DateTime.now());
		repository.refresh();
	}
}
