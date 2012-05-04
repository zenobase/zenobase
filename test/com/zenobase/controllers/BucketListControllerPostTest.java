package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;
import play.test.Helpers;

import com.zenobase.commands.CreateBucketCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Permission;

public class BucketListControllerPostTest extends BucketListControllerTestSupport {

	@Test
	public void testPostToCreateBucket() {
		ArgumentCaptor<CreateBucketCommand> arg = ArgumentCaptor.forClass(CreateBucketCommand.class);
		String commandId = Generator.id();
		String label = "test";
		String description = "just testing";
		when(auth.getPrincipal(true)).thenReturn(user.asIdentity());
		when(dispatcher.dispatch(arg.capture())).thenReturn(commandId);
		Result result = call(new CreateBucketForm(label, description).toJson());
		Bucket bucket = arg.getValue().getBucket();
		assertThat(bucket.getLabel()).isEqualTo(label);
		assertThat(bucket.getDescription()).isEqualTo(description);
		assertThat(bucket.getPermission(user.asIdentity())).isEqualTo(Permission.ALL);
		assertThat(Helpers.redirectLocation(result)).isEqualTo(com.zenobase.controllers.routes.BucketController.get(bucket.getId()).toString());
		assertThat(result).hasStatus(CREATED).hasContent(BucketListController.receipt(commandId));
	}

	@Test
	public void testCantCreateBucketWithoutLabel() {
		when(auth.getPrincipal(true)).thenReturn(user.asIdentity());
		Result result = call(Nodes.newObject());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(dispatcher);
	}

	private static Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.BucketListController.post(), fakeRequest().withJsonBody(body));
	}
}
