package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.when;
import static play.test.Helpers.callAction;

import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Quota;

public class QuotaControllerHttpGetTest extends QuotaControllerTestSupport {

	@Test
	public void test() {
		Quota expected = new Quota(1000, 50);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(quotas.getQuota(user.asIdentity())).thenReturn(expected);
		Result result = call(user.getId());
		assertThat(result).hasStatus(Http.Status.OK).hasContent(expected.toJson());
	}

	@Test
	public void testUnauthorized() {
		Result result = call(user.getId());
		assertThat(result).hasStatus(Http.Status.UNAUTHORIZED);
	}

	@Test
	public void testNotFound() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call("@nobody");
		assertThat(result).hasStatus(Http.Status.NOT_FOUND);
	}

	@Test
	public void testForbidden() {
		Identity someone = new Identity();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(someone.getId());
		assertThat(result).hasStatus(Http.Status.FORBIDDEN);
	}

	@Test
	public void testSuperuser() {
		Identity someone = new Identity();
		Quota expected = new Quota(1000, 50);
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(quotas.getQuota(someone)).thenReturn(expected);
		Result result = call(someone.getId());
		assertThat(result).hasStatus(Http.Status.OK).hasContent(expected.toJson());
	}

	private static Result call(String userId) {
		return callAction(com.zenobase.controllers.routes.ref.QuotaController.get(userId));
	}
}
