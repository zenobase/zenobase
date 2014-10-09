package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.verify;
import static play.mvc.Http.Status.NO_CONTENT;
import static play.test.Helpers.*;

import org.junit.Test;
import play.mvc.Result;
import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.models.StatusInfo;

public class StatusControllerHttpPostTest extends StatusControllerTestSupport {

	@Test
	public void test() {
		Result result = call(new StatusInfo(true).toJson());
		assertThat(result).hasStatus(NO_CONTENT);
		verify(hazelcast).setReadOnly(true);
	}

	private static Result call(JsonNode node) {
		return callAction(com.zenobase.controllers.routes.ref.StatusController.post(), fakeRequest().withJsonBody(node));
	}
}
