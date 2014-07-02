package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeApplication;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.EventRepository;
import com.zenobase.services.UserRepository;

public class TagControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final BucketRepository buckets = mock(BucketRepository.class);
	private final EventRepository events = mock(EventRepository.class);
	private final UserRepository users = mock(UserRepository.class);
	private final User user = new User("tester");
	private final Bucket bucket = new Bucket();

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(auth);
				bind(BucketRepository.class).toInstance(buckets);
				bind(EventRepository.class).toInstance(events);
				bind(UserRepository.class).toInstance(users);
				bind(TagController.class).in(Singleton.class);
			}
		});
	}

	@Before
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testGetTags() {
		List<String> tags = Lists.newArrayList("foo", "bar");
		bucket.setWidgets(ImmutableList.of(Nodes.newObject()));
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.terms(bucket.getId(), Event.TAG.getName())).thenReturn(tags);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(OK).hasContent(Nodes.newArray(tags));
	}

	@Test
	public void testGetBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(NOT_FOUND);
	}

	@Test
	public void testGetBucketUnauthorized() {
		when(auth.current()).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testGetBucketForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(FORBIDDEN);
	}

	private static Result call(String bucketId) {
		return callAction(com.zenobase.controllers.routes.ref.TagController.get(bucketId), fakeRequest());
	}
}
