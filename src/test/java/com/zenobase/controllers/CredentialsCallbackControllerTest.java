package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

public class CredentialsCallbackControllerTest extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(auth);
				bindConstant().annotatedWith(Names.named("web.hostname")).to("https://zenobase.com");
				bind(CredentialsCallbackController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		CredentialsCallbackController controller = injector.getInstance(CredentialsCallbackController.class);
		builder.get("/oauth/callback/{id}", controller::callback);
	}

	@Test
	public void testRedirect() {
		try (Http1ClientResponse result = call("0123456789", "a=b&c=d")) {
			assertThat(result)
				.hasStatus(303)
				.hasHeader("Location", "https://zenobase.com/#/credentials/0123456789?a=b&c=d");
		}
	}

	@Test
	public void testRedirectSentinel() {
		try (Http1ClientResponse result = call("-", "a=b")) {
			assertThat(result).hasStatus(303).hasHeader("Location", "https://zenobase.com/#/credentials/-?a=b");
		}
	}

	@Test
	public void testRejectsInvalidId() {
		try (Http1ClientResponse result = call("zzz", "a=b")) {
			assertThat(result).hasStatus(404);
		}
	}

	private Http1ClientResponse call(String taskId, String params) {
		var request = client.get("/oauth/callback/" + taskId);
		for (String param : params.split("&")) {
			String[] kv = param.split("=", 2);
			request = request.queryParam(kv[0], kv.length > 1 ? kv[1] : "");
		}
		return request.followRedirects(false).request();
	}
}
