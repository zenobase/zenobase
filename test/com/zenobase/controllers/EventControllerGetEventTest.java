package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.models.User;
import com.zenobase.services.BucketManager;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.IndexManager;

public class EventControllerGetEventTest {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final BucketManager buckets = mock(BucketManager.class);
	private final IndexManager indexes = mock(IndexManager.class);
	private final CommandQueue queue = mock(CommandQueue.class);
	private final User user = new User(Generator.id(), "tester");
	private final Bucket bucket = new Bucket();
	private final Event event = new Event();

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(BucketManager.class).toInstance(buckets);
				bind(IndexManager.class).toInstance(indexes);
				bind(CommandQueue.class).toInstance(queue);
				requestStaticInjection(EventController.class);
			}
		});
		bucket.addPermission(user.asIdentity(), Permission.ALL);
		event.setValue(Event.AUTHOR, user.asIdentity());
	}

	@Test
	public void testSuccess() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(OK).hasContent(event.toJson());
	}

	@Test
	public void testMissingBucket() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(null);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testMissingEvent() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(null);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.findEvent(bucket.getId(), event.getId())).thenReturn(event);
		Result result = call(bucket, event);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private Result call(Bucket bucket, Event event) {
		return callAction(com.zenobase.controllers.routes.ref.EventController.get(bucket.getId(), event.getId()), fakeRequest());
	}
}
