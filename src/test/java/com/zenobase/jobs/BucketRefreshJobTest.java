package com.zenobase.jobs;

import static com.zenobase.testing.CallbackAnswer.doCallback;
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
import com.zenobase.tasks.MissingCredentialsException;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskRefresher;
import java.util.List;
import org.junit.jupiter.api.Test;

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

		verify(refresher).refresh(task);
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
		verify(refresher, never()).refresh(any());
	}

	@Test
	public void testMissingOwner() {
		var owner = new User("alice");
		var bucket = new Bucket("bucket-1");

		bucket.addRole(owner.asIdentity(), Role.OWNER);
		doCallback(bucket).when(buckets).find(any(), any());
		when(users.find(owner.asIdentity())).thenReturn(null);

		job.run();

		verify(refresher, never()).refresh(any());
	}

	@Test
	public void testMissingCredentials() {
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
		doThrow(new MissingCredentialsException("foo")).when(refresher).refresh(task);

		job.run();

		verify(refresher).refresh(task);
	}

	@Test
	public void testRuntimeException() {
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
		doThrow(new RuntimeException("boom")).when(refresher).refresh(task);

		job.run();

		verify(refresher).refresh(task);
	}
}
