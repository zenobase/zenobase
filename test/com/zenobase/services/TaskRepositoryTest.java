package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;

import com.zenobase.common.Callback;
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
	public void test() {
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
	public void testFindWithPaging() {
		List<Task> expected = insert(11);
		assertThat(repository.find(0, 10)).hasTotal(expected.size()).isEqualTo(expected.subList(0, 10));
		assertThat(repository.find(10, 10)).hasTotal(expected.size()).isEqualTo(expected.subList(10, 11));
		assertThat(repository.find(20, 10)).hasTotal(expected.size()).isEqualTo(Collections.emptyList());
	}

	@Test
	public void testFindWithCallback() {
		List<Task> expected = insert(11);
		Callback<Task> callback = mock(Callback.class);
		repository.find(new TaskQuery(), callback);
		verifyInteractions(callback, expected);
	}

	@Test
	public void testFindBucketEqualTo() {
		Identity me = new Identity();
		Task t1 = insert("foo", me);
		insert("bar", me);
		Callback<Task> callback = mock(Callback.class);
		repository.find(new TaskQuery().bucketEqualTo(t1.getBucketId()), callback);
		verifyInteractions(callback, ImmutableList.of(t1));
	}

	@Test
	public void testFindPrincipalEqualTo() {
		String bucketId = "foo";
		Task t1 = insert(bucketId, new Identity());
		insert(bucketId, new Identity());
		Callback<Task> callback = mock(Callback.class);
		repository.find(new TaskQuery().principalEqualTo(t1.getPrincipal()), callback);
		verifyInteractions(callback, ImmutableList.of(t1));
	}

	private List<Task> insert(int size) {
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

	private Task insert(String bucketId, Identity principal) {
		Task task = new Task(type, bucketId, principal);
		Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS); // tasks will be returned in order of creation time
		repository.store(task, DateTime.now());
		repository.refresh();
		return task;
	}
}
