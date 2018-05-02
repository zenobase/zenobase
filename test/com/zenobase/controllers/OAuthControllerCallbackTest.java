package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.*;

import org.junit.Test;
import play.mvc.Result;

public class OAuthControllerCallbackTest extends OAuthControllerTestSupport {

	@Test
	public void testRedirect() {
		Result result = call("zzz", "?a=b&c=d");
		assertThat(result).hasStatus(SEE_OTHER).hasHeader("Location", "/#/credentials/zzz?a=b&c=d");
	}

	private Result call(String taskId, String params) {
		return callAction(com.zenobase.controllers.routes.ref.OAuthController.callback(taskId), fakeRequest("GET", params));
	}
}
