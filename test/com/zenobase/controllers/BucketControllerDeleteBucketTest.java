package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.models.User;
import com.zenobase.services.BucketManager;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.UserManager;

public class BucketControllerDeleteBucketTest {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final BucketManager buckets = mock(BucketManager.class);
	private final UserManager users = mock(UserManager.class);
	private final CommandQueue queue = mock(CommandQueue.class);
	private final User user = new User(Generator.id(), "tester");
	private final Bucket bucket = new Bucket();

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(BucketManager.class).toInstance(buckets);
				bind(UserManager.class).toInstance(users);
				bind(CommandQueue.class).toInstance(queue);
				requestStaticInjection(BucketController.class);
			}
		});
		bucket.setLabel("Obsolete Bucket");
		bucket.addPermission(user.asIdentity(), Permission.ALL);
	}

	@Test
	public void test() {
		String commandId = Generator.id();
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		when(queue.dispatch(any(DeleteBucketCommand.class))).thenReturn(commandId);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(OK).hasContent(BucketController.receipt(commandId));
	}

	@Test
	public void testAsSuperuser() {
		String commandId = Generator.id();
		Identity superuser = new Identity();
		when(auth.getPrincipal()).thenReturn(superuser);
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		when(queue.dispatch(any(DeleteBucketCommand.class))).thenReturn(commandId);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(OK).hasContent(BucketController.receipt(commandId));
	}

	@Test
	public void testNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(null);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(queue);
	}

	@Test
	public void testUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(queue);
	}

	@Test
	public void testForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(queue);
	}

	private Result call(String bucketId) {
		return callAction(com.zenobase.controllers.routes.ref.BucketController.delete(bucketId));
	}
}
