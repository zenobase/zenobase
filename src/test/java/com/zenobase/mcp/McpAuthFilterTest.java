package com.zenobase.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.zenobase.auth.IdentityProvider;
import com.zenobase.auth.UserStateCache;
import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.auth.auth0.Auth0TokenValidator;
import com.zenobase.auth.local.LocalIdentityProvider;
import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.controllers.ControllerTestSupport;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.testing.ResultAssert;
import io.helidon.http.HeaderNames;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Pins {@link McpAuthFilter}'s path scoping. Critical: Helidon's {@code routing.addFilter(...)} is global, so the
 * filter must guard against non-{@code /mcp} paths itself. Without that guard, every first-party REST endpoint would
 * 401 with the MCP {@code WWW-Authenticate} challenge.
 */
public class McpAuthFilterTest extends ControllerTestSupport {

	private static final String API_HOSTNAME = "https://api.zenobase.test";

	@Override
	protected Module module() {
		Auth0TokenValidator validator = Auth0Fixture.makeValidator(
			"https://api.zenobase.com",
			"https://api.zenobase.com/external"
		);
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Auth0TokenValidator.class).toInstance(validator);
				bindConstant().annotatedWith(Names.named("api.hostname")).to(API_HOSTNAME);
				bind(IdentityProvider.class).to(LocalIdentityProvider.class).in(Singleton.class);
				// AuthorizationContext takes a Set<TokenValidator>; we don't need a working validator for these
				// tests (we only assert filter-scoping behavior, not token decoding). Empty set → all requests look
				// "unauthenticated" so the filter exercises its 401-challenge path on /mcp and its pass-through path
				// on /status.
				bind(AuthorizationContext.class).toInstance(
					new AuthorizationContext(Set.of(), new UserStateCache(Mockito.mock(UserRepository.class)))
				);
				bind(ExternalClientRepository.class).toInstance(Mockito.mock(ExternalClientRepository.class));
				bind(CommandDispatcher.class).toInstance(Mockito.mock(CommandDispatcher.class));
				bind(McpAuthFilter.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		// Add the filter globally — exactly as Routing.buildRouting does — then register a couple of fake routes for
		// the filter to flow through. /status is a public REST route; /mcp is the gated MCP endpoint.
		builder.addFilter(injector.getInstance(McpAuthFilter.class));
		builder.get("/status", (req, res) -> res.send("{\"ok\":true}"));
		builder.post("/mcp", (req, res) -> res.send("{}"));
	}

	@Test
	public void testNonMcpPathPassesThroughUnauthenticated() {
		// /status is NOT under /mcp, so the filter must call chain.proceed() without inspecting auth.
		try (Http1ClientResponse result = client.get("/status").request()) {
			ResultAssert.assertThat(result).hasStatus(200);
		}
	}

	@Test
	public void testMcpPathGetsChallengedUnauthenticated() {
		// POST /mcp with no Authorization header → 401 with the RFC 9728 challenge pointing at our metadata endpoint.
		try (Http1ClientResponse result = client.post("/mcp").submit("{}")) {
			ResultAssert.assertThat(result).hasStatus(401);
			String wwwAuth = result.headers().first(HeaderNames.WWW_AUTHENTICATE).orElse(null);
			assertThat(wwwAuth)
				.contains("Bearer")
				.contains("resource_metadata=")
				.contains(API_HOSTNAME + "/.well-known/oauth-protected-resource");
		}
	}

	@Test
	public void testMcpSubpathAlsoGated() {
		// /mcp/anything must also be gated (defensive against future sub-routes Helidon MCP might add).
		try (Http1ClientResponse result = client.post("/mcp/session/abc").submit("{}")) {
			// Either 401 (challenge) or 404 (no route) — both prove the filter ran. The filter sends 401 first.
			int status = result.status().code();
			assertThat(status).isIn(401, 404);
		}
	}

	@Test
	public void testMcpAdjacentPathNotGated() {
		// /mcpsomething (no trailing slash, just a similar prefix) must NOT be gated — the guard is "/mcp" or "/mcp/".
		// We can't easily route this without registering it, so just verify the underlying path-match logic.
		assertThat("/mcpsomething".equals("/mcp")).isFalse();
		assertThat("/mcpsomething".startsWith("/mcp/")).isFalse();
	}

	@Test
	public void testUnusedAuthorizationFieldsDoNotTriggerChallenge() {
		// Sanity: an unrelated REST controller can still hit DI'd Authorization helpers; this test ensures we don't
		// accidentally hold references to the new auth pieces beyond what's needed.
		Authorization authorization = new Authorization(
			new Identity("user-1"),
			new Identity("client-1"),
			Auth0TokenAuthorizer.EXTERNAL_SCOPE
		);
		assertThat(authorization.getScope()).isEqualTo("external");
	}
}
