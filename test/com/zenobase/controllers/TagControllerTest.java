package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.UserRepository;

public class TagControllerTest extends ControllerTestSupport {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final BucketRepository buckets = mock(BucketRepository.class);
	private final UserRepository users = mock(UserRepository.class);
	private final User user = new User("tester");
	private final Bucket bucket = new Bucket();

	@Before
	public void setUp() {
		start(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(BucketRepository.class).toInstance(buckets);
				bind(UserRepository.class).toInstance(users);
				bind(TagController.class).in(Singleton.class);
			}
		});
		bucket.addPermission(user.asIdentity(), Permission.ALL);
	}

	@Test
	public void testGetTags() {
		List<String> tags = Lists.newArrayList("foo", "bar");
		bucket.setWidgets(ImmutableList.of(Nodes.newObject()));
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket);
		when(buckets.terms(bucket.getId(), Event.TAG.getName())).thenReturn(tags);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(OK).hasContent(Nodes.newArray(tags));
	}

	@Test
	public void testGetBucketNotFound() {
		when(auth.getPrincipal()).thenReturn(user.asIdentity());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetBucketUnauthorized() {
		when(auth.getPrincipal()).thenReturn(null);
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetBucketForbidden() {
		when(auth.getPrincipal()).thenReturn(new Identity());
		when(buckets.findBucket(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String bucketId) {
		return callAction(com.zenobase.controllers.routes.ref.TagController.get(bucketId), fakeRequest());
	}
}
