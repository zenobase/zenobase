package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;
import play.test.Helpers;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;

public class BucketListControllerHttpPostTest extends BucketListControllerTestSupport {

	@Test
	public void testCreateBucket() {
		ArgumentCaptor<CreateBucketCommand> arg = ArgumentCaptor.forClass(CreateBucketCommand.class);
		String commandId = Generator.id();
		String label = "test";
		String description = "just testing";
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		Result result = call(new CreateBucketForm(label, description, ImmutableList.<Alias>of()).toJson());
		assertThat(result).hasStatus(CREATED).hasHeader(COMMAND_ID, commandId);
		Bucket bucket = arg.getValue().getBucket();
		assertThat(result).hasContent(bucket.toJson());
		assertThat(bucket.getLabel()).isEqualTo(label);
		assertThat(bucket.getDescription()).isEqualTo(description);
		assertThat(bucket.hasRole(new Authorization(user.asIdentity()), Role.OWNER)).isTrue();
		assertThat(Helpers.redirectLocation(result)).isEqualTo(com.zenobase.controllers.routes.BucketController.get(bucket.getId(), false).toString());
	}

	@Test
	public void testCreateVirtualBucket() {
		ArgumentCaptor<CreateBucketCommand> arg = ArgumentCaptor.forClass(CreateBucketCommand.class);
		String commandId = Generator.id();
		String label = "test";
		Bucket alias = new Bucket();
		alias.addRole(user.asIdentity(), Role.OWNER);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		when(buckets.find(alias.getId())).thenReturn(alias);
		Result result = call(new CreateBucketForm(label, null, ImmutableList.of(new Alias(alias.getId()))).toJson());
		assertThat(result).hasStatus(CREATED).hasHeader(COMMAND_ID, commandId);
		Bucket bucket = arg.getValue().getBucket();
		assertThat(result).hasContent(bucket.toJson());
		assertThat(bucket.getLabel()).isEqualTo(label);
		assertThat(bucket.getAliases()).containsExactly(new Alias(alias.getId()));
		assertThat(bucket.hasRole(new Authorization(user.asIdentity()), Role.OWNER)).isTrue();
		assertThat(Helpers.redirectLocation(result)).isEqualTo(com.zenobase.controllers.routes.BucketController.get(bucket.getId(), false).toString());
	}

	@Test
	public void testCreateBucketWithoutLabel() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(Nodes.newObject());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testCreateBucketUnauthorized() {
		when(auth.current()).thenReturn(null);
		Result result = call(Nodes.newObject());
		assertThat(result).hasStatus(UNAUTHORIZED);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testCreateVirtualBucketWithUnauthorizedAlias() {
		ArgumentCaptor<CreateBucketCommand> arg = ArgumentCaptor.forClass(CreateBucketCommand.class);
		String commandId = Generator.id();
		String label = "test";
		Bucket alias = new Bucket();
		alias.addRole(new Identity(), Role.OWNER);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		when(buckets.find(alias.getId())).thenReturn(alias);
		Result result = call(new CreateBucketForm(label, null, ImmutableList.of(new Alias(alias.getId()))).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	@Test
	public void testCreateVirtualBucketWithMissingAlias() {
		ArgumentCaptor<CreateBucketCommand> arg = ArgumentCaptor.forClass(CreateBucketCommand.class);
		String commandId = Generator.id();
		String label = "test";
		Bucket alias = new Bucket();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		when(buckets.find(alias.getId())).thenReturn(null);
		Result result = call(new CreateBucketForm(label, null, ImmutableList.of(new Alias(alias.getId()))).toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.BucketListController.post(), fakeRequest().withJsonBody(body));
	}
}
