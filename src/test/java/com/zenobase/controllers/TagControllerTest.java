package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import com.google.common.collect.Lists;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
	protected void routing(HttpRouting.Builder builder) {
		var controller = new TagController(auth, buckets, events);
		builder.get("/buckets/{bucketId}/tags/", controller::get);
	}

	@BeforeEach
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testGetTags() {
		List<String> tags = Lists.newArrayList("foo", "bar");
		bucket.setWidgets(List.of(Nodes.newObject()));
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.terms(bucket.getId(), Event.TAG.getName())).thenReturn(tags);
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(200).hasContent(Nodes.newArray(tags));
		}
	}

	@Test
	public void testGetBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(404);
		}
	}

	@Test
	public void testGetBucketUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testGetBucketForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(403);
		}
	}

	private Http1ClientResponse call(String bucketId) {
		return client.get("/buckets/" + bucketId + "/tags/").request();
	}
}
