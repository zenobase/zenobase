package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.Test;

import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;
import com.zenobase.services.UserRepository;

public class RedirectControllerTest extends ControllerTestSupport {

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(mock(AuthorizationContext.class));
				bind(UserRepository.class).toInstance(mock(UserRepository.class));
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		RedirectController controller = injector.getInstance(RedirectController.class);
		builder.get("/to", controller::get);
	}

	@Test
	public void testUser() {
		String url = "https://zenobase.com/";
		try (Http1ClientResponse result = client.get("/to").queryParam("url", url).followRedirects(false).request()) {
			assertThat(result).hasStatus(302).hasHeader("Location", url).isEmpty();
		}
	}
}
