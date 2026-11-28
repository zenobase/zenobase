package com.zenobase.jobs;

import static com.zenobase.testing.CallbackAnswer.doCallback;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.queries.TaskQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.MissingCredentialsException;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskRefresher;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BucketRefreshJobTest {

	private final BucketRepository buckets = mock(BucketRepository.class);
	private final UserRepository users = mock(UserRepository.class);
	private final TaskRepository tasks = mock(TaskRepository.class);
	private final TaskRefresher refresher = mock(TaskRefresher.class);
	private final BucketRefreshJob job = new BucketRefreshJob(buckets, users, tasks, refresher);

	@Test
	public void test() {
		var owner = new User("alice");
		owner.setQuota(100);
		var bucket = new Bucket("bucket-1");
		bucket.addRole(owner.asIdentity(), Role.OWNER);
		var task = new Task("foo", bucket.getId(), owner.asIdentity());

		doCallback(bucket).when(buckets).find(any(), any());
		when(users.find(owner.asIdentity())).thenReturn(owner);
		when(tasks.find(eq(new TaskQuery().bucketEqualTo(bucket.getId())), any(), eq(0), eq(100))).thenReturn(
			DefaultPartialList.of(List.of(task), 1)
		);

		job.run();

		verify(refresher).refresh(task, bucket);
	}

	@Test
	public void testInsufficientQuota() {
		var owner = new User("alice");
		var bucket = new Bucket("bucket-1");
		bucket.addRole(owner.asIdentity(), Role.OWNER);

		doCallback(bucket).when(buckets).find(any(), any());
		when(users.find(owner.asIdentity())).thenReturn(owner);

		job.run();

		verify(tasks, never()).find(any(TaskQuery.class), any(), eq(0), eq(100));
		verify(refresher, never()).refresh(any(), any());
	}

	@Test
	public void testMissingOwner() {
		var owner = new User("alice");
		var bucket = new Bucket("bucket-1");

		bucket.addRole(owner.asIdentity(), Role.OWNER);
		doCallback(bucket).when(buckets).find(any(), any());
		when(users.find(owner.asIdentity())).thenReturn(null);

		job.run();

		verify(refresher, never()).refresh(any(), any());
	}

	static Stream<Throwable> failures() {
		return Stream.of(
			new MissingCredentialsException("foo"),
			new InvalidStatusException("https://example.com", 503, "unavailable"),
			new RuntimeException("boom"),
			new NoClassDefFoundError("javax/xml/bind/DatatypeConverter")
		);
	}

	@ParameterizedTest
	@MethodSource("failures")
	public void testFailureDoesNotStopOtherBuckets(Throwable failure) {
		var owner = new User("alice");
		owner.setQuota(100);
		var failing = new Bucket("bucket-1");
		failing.addRole(owner.asIdentity(), Role.OWNER);
		var failingTask = new Task("foo", failing.getId(), owner.asIdentity());
		var healthy = new Bucket("bucket-2");
		healthy.addRole(owner.asIdentity(), Role.OWNER);
		var healthyTask = new Task("foo", healthy.getId(), owner.asIdentity());

		doCallback(failing, healthy).when(buckets).find(any(), any());
		when(users.find(owner.asIdentity())).thenReturn(owner);
		when(tasks.find(eq(new TaskQuery().bucketEqualTo(failing.getId())), any(), eq(0), eq(100))).thenReturn(
			DefaultPartialList.of(List.of(failingTask), 1)
		);
		when(tasks.find(eq(new TaskQuery().bucketEqualTo(healthy.getId())), any(), eq(0), eq(100))).thenReturn(
			DefaultPartialList.of(List.of(healthyTask), 1)
		);
		doThrow(failure).when(refresher).refresh(failingTask, failing);

		job.run();

		verify(refresher).refresh(healthyTask, healthy);
	}

	@Test
	public void testVirtualMachineErrorIsRethrown() {
		var owner = new User("alice");
		owner.setQuota(100);
		var bucket = new Bucket("bucket-1");
		bucket.addRole(owner.asIdentity(), Role.OWNER);
		var task = new Task("foo", bucket.getId(), owner.asIdentity());

		doCallback(bucket).when(buckets).find(any(), any());
		when(users.find(owner.asIdentity())).thenReturn(owner);
		when(tasks.find(eq(new TaskQuery().bucketEqualTo(bucket.getId())), any(), eq(0), eq(100))).thenReturn(
			DefaultPartialList.of(List.of(task), 1)
		);
		doThrow(new OutOfMemoryError()).when(refresher).refresh(task, bucket);

		assertThrows(OutOfMemoryError.class, job::run);
	}
}
