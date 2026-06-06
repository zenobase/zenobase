package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.zenobase.json.Field;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.EventRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BucketSchemaControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final BucketRepository buckets = mock(BucketRepository.class);
	private final EventRepository events = mock(EventRepository.class);
	private final UserRepository users = mock(UserRepository.class);
	private final User user = new User("tester");
	private final Bucket bucket = new Bucket();

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(BucketRepository.class).toInstance(buckets);
				bind(EventRepository.class).toInstance(events);
				bind(UserRepository.class).toInstance(users);
				bind(BucketSchemaController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		BucketSchemaController controller = injector.getInstance(BucketSchemaController.class);
		builder.get("/buckets/{bucketId}/schema", controller::get);
	}

	@BeforeEach
	public void setUp() {
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testGetBucketSchema() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket);
		when(events.fields(bucket.getId())).thenReturn(List.<Field<?>>of(Event.ID, Event.TAG));
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(200).hasContent(
				Nodes.readObject(
					"""
					{"type":"object","properties":{\
					"@id":{"type":"string","readOnly":true},\
					"tag":{"oneOf":[\
					{"type":"string"},\
					{"type":"array","items":{"type":"string"}}]}}}"""
				)
			);
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
		return client.get("/buckets/" + bucketId + "/schema").request();
	}
}
