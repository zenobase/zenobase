package com.zenobase.repositories;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.zenobase.common.Callback;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.queries.TaskQuery;
import com.zenobase.tasks.Task;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskRepositoryTest extends OpenSearchTestSupport {

	private static final String TYPE = "test";
	private static final String BUCKET = Generator.id();
	private static final Identity ME = new Identity();
	private static final Identity YOU = new Identity();

	private TaskRepository repository;

	@BeforeEach
	public void setUp() {
		repository = new TaskRepository(getManager());
	}

	@Test
	public void test() {
		Task task = new Task(TYPE, BUCKET, ME);
		assertThat(repository.find(task.getId())).isNull();
		assertThat(repository.delete(task.getId())).isFalse();
		repository.store(task);
		assertThat(repository.find(task.getId()).toJson()).isEqualTo(task.toJson());
		task.setCompleted(DateTime.now());
		repository.update(task);
		assertThat(repository.find(task.getId()).toJson()).isEqualTo(task.toJson());
		assertThat(repository.delete(task.getId())).isTrue();
		assertThat(repository.find(task.getId())).isNull();
	}

	@Test
	public void testFindWithPaging() {
		List<Task> expected = insert(11);
		assertThat(repository.find(0, 10)).hasTotal(expected.size()).isEqualTo(expected.subList(0, 10));
		assertThat(repository.find(10, 10)).hasTotal(expected.size()).isEqualTo(expected.subList(10, 11));
		assertThat(repository.find(20, 10)).hasTotal(expected.size()).isEmpty();
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
		Task t1 = insert("foo", ME);
		Task t2 = insert("foo", YOU);
		insert("bar", ME);
		Callback<Task> callback = mock(Callback.class);
		repository.find(new TaskQuery().bucketEqualTo("foo"), callback);
		verifyInteractions(callback, List.of(t1, t2));
	}

	@Test
	public void testFindPrincipalEqualTo() {
		Task t1 = insert("foo", ME);
		Task t2 = insert("bar", ME);
		insert("bar", YOU);
		Callback<Task> callback = mock(Callback.class);
		repository.find(new TaskQuery().principalEqualTo(t1.getPrincipal()), callback);
		verifyInteractions(callback, List.of(t1, t2));
	}

	private List<Task> insert(int size) {
		List<Task> tasks = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; ++i) {
			Task task = new Task(TYPE, BUCKET, ME);
			tasks.add(task);
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS); // sleep so we can sort by creation time later
			repository.store(task);
		}
		repository.refresh();
		return Lists.reverse(tasks);
	}

	private Task insert(String bucketId, Identity principal) {
		Task task = new Task(TYPE, bucketId, principal);
		repository.store(task);
		repository.refresh();
		return task;
	}
}
