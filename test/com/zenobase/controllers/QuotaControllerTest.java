package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.test.Helpers.callAction;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Quota;
import com.zenobase.services.QuotaManager;

public class QuotaControllerTest extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final QuotaManager quotas = mock(QuotaManager.class);
	protected final Identity user = new Identity();

	@Before
	public void setUp() {
		start(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(auth);
				bind(QuotaManager.class).toInstance(quotas);
				bind(QuotaController.class).in(Singleton.class);
			}
		});
	}

	@Test
	public void test() {
		Quota expected = new Quota(1000, 50);
		when(auth.current()).thenReturn(new Authorization(user));
		when(quotas.getQuota(user)).thenReturn(expected);
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
