package com.zenobase.mcp;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.zenobase.auth.auth0.Auth0TokenValidator;
import com.zenobase.controllers.ControllerTestSupport;
import com.zenobase.testing.ResultAssert;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the {@code /.well-known/oauth-protected-resource} endpoint advertises the canonical MCP endpoint URL
 * as {@code resource} (not the Auth0 API audience — see commit c2fee692) and 404s when external audience isn't
 * configured. Uses a real (non-Mockito) test JWKS so the {@link Auth0TokenValidator} construction succeeds.
 */
public class ProtectedResourceMetadataControllerTest extends ControllerTestSupport {

	private static final String API_HOSTNAME = "https://api.zenobase.test";
	private static final String AUDIENCE = "https://api.zenobase.com";
	private static final String EXTERNAL = "https://api.zenobase.com/external";

	private String configuredExternalAudience = EXTERNAL;

	@Override
	protected Module module() {
		Auth0TokenValidator validator = Auth0Fixture.makeValidator(AUDIENCE, configuredExternalAudience);
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Auth0TokenValidator.class).toInstance(validator);
				bindConstant().annotatedWith(Names.named("api.hostname")).to(API_HOSTNAME);
				bind(ProtectedResourceMetadataController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		ProtectedResourceMetadataController controller = injector.getInstance(
			ProtectedResourceMetadataController.class
		);
		builder.get("/.well-known/oauth-protected-resource", controller::get);
	}

	@Test
	public void testResourceFieldPointsAtMcpEndpoint() {
		try (Http1ClientResponse result = client.get("/.well-known/oauth-protected-resource").request()) {
			ResultAssert.assertThat(result)
				.hasStatus(200)
				.asObjectNode()
				.path("resource")
				.isEqualTo(API_HOSTNAME + "/mcp");
		}
	}

	@Test
	public void testReturns404WhenExternalAudienceUnconfigured() {
		configuredExternalAudience = "";
		setUpClient();
		try (Http1ClientResponse result = client.get("/.well-known/oauth-protected-resource").request()) {
			ResultAssert.assertThat(result).hasStatus(404);
		}
	}
}
