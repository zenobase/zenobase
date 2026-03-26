package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenobase.commands.Command;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.common.Callback;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.AuthorizationQuery;
import com.zenobase.services.TaskQuery;

public class BucketControllerHttpDeleteTest extends BucketControllerTestSupport {

	private Bucket bucket = new Bucket();

	@BeforeEach
	public void setUp() {
		bucket.setLabel("Obsolete Bucket");
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testDeleteBucket() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		when(buckets.isAliased(bucket.getId())).thenReturn(false);
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
			verify(authorizations).find(eq(new AuthorizationQuery().scopeEqualTo(bucket.getId())), any(Callback.class));
			verify(tasks).find(eq(new TaskQuery().bucketEqualTo(bucket.getId())), any(Callback.class));
		}
	}

	@Test
	public void testDeleteBucketSignedInAsSuperuser() {
		String commandId = Generator.id();
		Identity superuser = new Identity();
		when(auth.current(any())).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		when(buckets.isAliased(bucket.getId())).thenReturn(false);
		when(dispatcher.dispatch(any(DeleteBucketCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
			verify(authorizations).find(eq(new AuthorizationQuery().scopeEqualTo(bucket.getId())), any(Callback.class));
			verify(tasks).find(eq(new TaskQuery().bucketEqualTo(bucket.getId())), any(Callback.class));
		}
	}

	@Test
	public void testDeleteBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testDeleteBucketNotSignedIn() {
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testDeleteBucketNotPermitted() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testDeleteAliasedBucket() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		when(buckets.isAliased(bucket.getId())).thenReturn(true);
		try (Http1ClientResponse result = call(bucket.getId())) {
			assertThat(result).hasStatus(409);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(String bucketId) {
		return client.delete("/buckets/" + bucketId).request();
	}
}
