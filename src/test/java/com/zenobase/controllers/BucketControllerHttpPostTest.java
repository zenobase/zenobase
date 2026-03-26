package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;

import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;

public class BucketControllerHttpPostTest extends BucketControllerTestSupport {

	private Bucket from, to;

	@BeforeEach
	public void setUp() {
		from = new Bucket();
		from.setLabel("Test Bucket");
		from.addRole(user.asIdentity(), Role.OWNER);
		to = from.copy();
		to.setLabel("Real Bucket");
	}

	@Test
	public void testUpdateBucket() {
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(dispatcher.dispatch(any(UpdateBucketCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testAddRole() {
		user.setVerified(true);
		to.addRole(Identity.PUBLIC, Role.VIEWER);
		String commandId = Generator.id();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(users.find(user.asIdentity())).thenReturn(user);
		when(dispatcher.dispatch(any(UpdateBucketCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	@Test
	public void testAddRoleAsGuest() {
		to.addRole(Identity.PUBLIC, Role.VIEWER);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testAddRoleAsUnverifiedUser() {
		to.addRole(Identity.PUBLIC, Role.VIEWER);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConflict() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(dispatcher.dispatch(any(UpdateBucketCommand.class)))
				.thenThrow(new OpenSearchException(ErrorResponse.of(r -> r.status(409)
						.error(e2 ->
								e2.type("version_conflict_engine_exception").reason("version conflict")))));
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(409);
		}
	}

	@Test
	public void testUpdateBucketInvalidLabel() {
		to.setLabel("");
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateBucketAddOwner() {
		to.addRole(new Identity(), Role.OWNER);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateBucketNotFound() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(404);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateBucketUnauthorized() {
		when(buckets.find(from.getId())).thenReturn(from.copy());
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateBucketForbidden() {
		when(auth.current(any())).thenReturn(new Authorization(new Identity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(403);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testUpdateBucketChangeType() {
		to.addAlias(new Alias("foo"));
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testUpdateBucketInvalidAlias() {
		from.addAlias(new Alias("foo"));
		to.addAlias(new Alias("bar"));
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(buckets.find("bar")).thenReturn(new Bucket("bar"));
		when(users.find(user.asIdentity())).thenReturn(user);
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testUpdateBucketReplaceAlias() {
		String commandId = Generator.id();
		Bucket alias = new Bucket("bar");
		alias.addRole(user.asIdentity(), Role.OWNER);
		from.addAlias(new Alias("foo"));
		to.addAlias(new Alias(alias.getId()));
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(buckets.find("bar")).thenReturn(alias);
		when(users.find(user.asIdentity())).thenReturn(user);
		when(dispatcher.dispatch(any(UpdateBucketCommand.class))).thenReturn(commandId);
		try (Http1ClientResponse result = call(from.getId(), to.toJson())) {
			assertThat(result).hasStatus(204).hasHeader(COMMAND_ID, commandId).isEmpty();
		}
	}

	private Http1ClientResponse call(String bucketId, ObjectNode body) {
		return client.put("/buckets/" + bucketId).submit(body);
	}
}
