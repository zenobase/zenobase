package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;

import com.zenobase.oauth.Authorization;
import com.zenobase.services.Quota;

public class QuotaControllerHttpGetTest extends QuotaControllerTestSupport {

	@Test
	public void test() {
		Quota expected = new Quota(1000, 50);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(quotas.getQuota(user.asIdentity())).thenReturn(expected);
		Result result = call();
		assertThat(result).hasStatus(Http.Status.OK).hasContent(expected.toJson());
	}

	@Test
	public void testAnonymous() {
		Result result = call();
		assertThat(result).hasStatus(Http.Status.NO_CONTENT);
	}

	private static Result call() {
		return callAction(com.zenobase.controllers.routes.ref.QuotaController.get());
	}
}
