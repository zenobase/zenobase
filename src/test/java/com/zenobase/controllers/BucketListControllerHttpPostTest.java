package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import io.helidon.webclient.http1.Http1ClientResponse;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		try (Http1ClientResponse result = call(new CreateBucketForm(label, description, List.of()).toJson())) {
			assertThat(result).hasStatus(201).hasHeader(COMMAND_ID, commandId);
			Bucket bucket = arg.getValue().getBucket();
			assertThat(result).hasContent(bucket.toJson());
			assertThat(bucket.getLabel()).isEqualTo(label);
			assertThat(bucket.getDescription()).isEqualTo(description);
			assertThat(bucket.hasRole(new Authorization(user.asIdentity()), Role.OWNER)).isTrue();
			assertThat(result).hasHeader("Location", "/buckets/" + bucket.getId());
			assertThat(result).hasHeader(COMMAND_ID, commandId);
		}
	}

	@Test
	public void testCreateVirtualBucket() {
		ArgumentCaptor<CreateBucketCommand> arg = ArgumentCaptor.forClass(CreateBucketCommand.class);
		String commandId = Generator.id();
		String label = "test";
		Bucket alias = new Bucket();
		alias.addRole(user.asIdentity(), Role.OWNER);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		when(buckets.find(alias.getId())).thenReturn(alias);
		try (Http1ClientResponse result = call(new CreateBucketForm(label, null, List.of(new Alias(alias.getId()))).toJson())) {
			assertThat(result).hasStatus(201).hasHeader(COMMAND_ID, commandId);
			Bucket bucket = arg.getValue().getBucket();
			assertThat(result).hasContent(bucket.toJson());
			assertThat(bucket.getLabel()).isEqualTo(label);
			assertThat(bucket.getAliases()).containsExactly(new Alias(alias.getId()));
			assertThat(bucket.hasRole(new Authorization(user.asIdentity()), Role.OWNER)).isTrue();
			assertThat(result).hasHeader("Location", "/buckets/" + bucket.getId());
			assertThat(result).hasHeader(COMMAND_ID, commandId);
		}
	}

	@Test
	public void testCreateInvalidVirtualBucket() {
		String label = "test";
		Bucket alias = new Bucket();
		alias.addRole(new Identity(), Role.OWNER);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(buckets.find(alias.getId())).thenReturn(alias);
		try (Http1ClientResponse result = call(new CreateBucketForm(label, null, List.of(new Alias(alias.getId()))).toJson())) {
			assertThat(result).hasStatus(400);
		}
	}

	@Test
	public void testCreateBucketWithoutLabel() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(Nodes.newObject())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testCreateBucketUnauthorized() {
		when(auth.current(any())).thenReturn(null);
		try (Http1ClientResponse result = call(Nodes.newObject())) {
			assertThat(result).hasStatus(401);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testCreateVirtualBucketWithUnauthorizedAlias() {
		ArgumentCaptor<CreateBucketCommand> arg = ArgumentCaptor.forClass(CreateBucketCommand.class);
		String commandId = Generator.id();
		String label = "test";
		Bucket alias = new Bucket();
		alias.addRole(new Identity(), Role.OWNER);
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		when(buckets.find(alias.getId())).thenReturn(alias);
		try (Http1ClientResponse result = call(new CreateBucketForm(label, null, List.of(new Alias(alias.getId()))).toJson())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	@Test
	public void testCreateVirtualBucketWithMissingAlias() {
		ArgumentCaptor<CreateBucketCommand> arg = ArgumentCaptor.forClass(CreateBucketCommand.class);
		String commandId = Generator.id();
		String label = "test";
		Bucket alias = new Bucket();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		when(buckets.find(alias.getId())).thenReturn(null);
		try (Http1ClientResponse result = call(new CreateBucketForm(label, null, List.of(new Alias(alias.getId()))).toJson())) {
			assertThat(result).hasStatus(400);
			verifyNoInteractions(dispatcher);
		}
	}

	private Http1ClientResponse call(ObjectNode body) {
		return client.post("/buckets/").submit(body);
	}
}
