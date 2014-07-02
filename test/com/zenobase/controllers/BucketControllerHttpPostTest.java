package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.UpdateBucketCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;

public class BucketControllerHttpPostTest extends BucketControllerTestSupport {

	private Bucket from, to;

	@Before
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
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(dispatcher.dispatch(any(UpdateBucketCommand.class))).thenReturn(commandId);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testAddRole() {
		user.setVerified(true);
		to.addRole(Identity.PUBLIC, Role.VIEWER);
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(users.find(user.asIdentity())).thenReturn(user);
		when(dispatcher.dispatch(any(UpdateBucketCommand.class))).thenReturn(commandId);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testAddRoleAsGuest() {
		to.addRole(Identity.PUBLIC, Role.VIEWER);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testAddRoleAsUnverifiedUser() {
		to.addRole(Identity.PUBLIC, Role.VIEWER);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConflict() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(dispatcher.dispatch(any(UpdateBucketCommand.class))).thenThrow(VersionConflictEngineException.class);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(CONFLICT);
	}

	@Test
	public void testUpdateBucketInvalidLabel() {
		to.setLabel("");
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketAddOwner() {
		to.addRole(new Identity(), Role.OWNER);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketUnauthorized() {
		when(buckets.find(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketForbidden() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testUpdateBucketChangeType() {
		to.addAlias(new Alias("foo"));
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testUpdateBucketInvalidAlias() {
		from.addAlias(new Alias("foo"));
		to.addAlias(new Alias("bar"));
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(buckets.find("bar")).thenReturn(new Bucket("bar"));
		when(users.find(user.asIdentity())).thenReturn(user);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	@Test
	public void testUpdateBucketReplaceAlias() {
		String commandId = Generator.id();
		Bucket alias = new Bucket("bar");
		alias.addRole(user.asIdentity(), Role.OWNER);
		from.addAlias(new Alias("foo"));
		to.addAlias(new Alias(alias.getId()));
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(from.getId())).thenReturn(from.copy());
		when(buckets.find("bar")).thenReturn(alias);
		when(users.find(user.asIdentity())).thenReturn(user);
		when(dispatcher.dispatch(any(UpdateBucketCommand.class))).thenReturn(commandId);
		Result result = call(from.getId(), to.toJson());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	private static Result call(String bucketId, ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.BucketController.update(bucketId), fakeRequest().withJsonBody(body));
	}
}
