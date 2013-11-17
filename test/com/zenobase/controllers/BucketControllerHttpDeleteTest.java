package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.commands.DeleteBucketCommand;
import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskList;

public class BucketControllerHttpDeleteTest extends BucketControllerTestSupport {

	private Bucket bucket = new Bucket();

	@Before
	@Override
	public void setUp() {
		super.setUp();
		bucket.setLabel("Obsolete Bucket");
		bucket.addRole(user.asIdentity(), Role.OWNER);
	}

	@Test
	public void testDeleteBucket() {
		String commandId = Generator.id();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		when(buckets.isAliased(bucket.getId())).thenReturn(false);
		when(authorizations.findAll(null, null, bucket.getId())).thenReturn(DefaultPartialList.<Authorization>of());
		when(tasks.find(Task.BUCKET.getName(), bucket.getId(), 0, 100)).thenReturn(new TaskList(DefaultPartialList.<ObjectNode>of()));
		when(dispatcher.dispatch(any(Command.class))).thenReturn(commandId);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testDeleteBucketSignedInAsSuperuser() {
		String commandId = Generator.id();
		Identity superuser = new Identity();
		when(auth.current()).thenReturn(new Authorization(superuser));
		when(users.isSuperuser(superuser)).thenReturn(true);
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		when(buckets.isAliased(bucket.getId())).thenReturn(false);
		when(authorizations.findAll(null, null, bucket.getId())).thenReturn(DefaultPartialList.<Authorization>of());
		when(tasks.find(Task.BUCKET.getName(), bucket.getId(), 0, 100)).thenReturn(new TaskList(DefaultPartialList.<ObjectNode>of()));
		when(dispatcher.dispatch(any(DeleteBucketCommand.class))).thenReturn(commandId);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(NO_CONTENT).hasHeader(COMMAND_ID, commandId).isEmpty();
	}

	@Test
	public void testDeleteBucketNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(NOT_FOUND);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteBucketNotSignedIn() {
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteBucketNotPermitted() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(FORBIDDEN);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testDeleteAliasedBucket() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(bucket.getId())).thenReturn(bucket.copy());
		when(buckets.isAliased(bucket.getId())).thenReturn(true);
		Result result = call(bucket.getId());
		assertThat(result).hasStatus(CONFLICT);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(String bucketId) {
		return callAction(com.zenobase.controllers.routes.ref.BucketController.delete(bucketId));
	}
}
