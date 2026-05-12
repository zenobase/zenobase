package com.zenobase.mcp;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.zenobase.auth.UserStateCache;
import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.auth.auth0.Auth0TokenValidator;
import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.controllers.ControllerTestSupport;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.ExternalClientRepository;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

/**
 * HTTP-level test for the {@code POST /mcp} entry controller — auth gating, RFC 9728 challenge, suspended-user
 * handling, and successful dispatch into {@link McpJsonRpcHandler}. The handler itself is exercised by
 * {@link McpJsonRpcHandlerTest}; here we just confirm the McpController plumbing.
 */
public class McpControllerTest extends ControllerTestSupport {

	private static final String API_HOSTNAME = "https://api.zenobase.test";

	private final AuthorizationContext authContext = mock(AuthorizationContext.class);
	private final McpJsonRpcHandler handler = mock(McpJsonRpcHandler.class);
	private final ExternalClientRepository clients = mock(ExternalClientRepository.class);
	private final Auth0TokenValidator validator = Auth0Fixture.makeValidator(
		"https://api.zenobase.com",
		"https://api.zenobase.com/external"
	);

	private final Identity user = new Identity("user-1");
	private final Identity clientId = new Identity("client-1");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(authContext);
				bind(McpJsonRpcHandler.class).toInstance(handler);
				bind(ExternalClientRepository.class).toInstance(clients);
				bind(Auth0TokenValidator.class).toInstance(validator);
				bindConstant().annotatedWith(Names.named("api.hostname")).to(API_HOSTNAME);
				bind(McpController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		McpController controller = injector.getInstance(McpController.class);
		builder.post("/mcp", controller::post);
	}

	@Test
	public void testUnauthenticatedReturns401WithRfc9728Challenge() {
		when(authContext.current(any())).thenReturn(null);
		try (Http1ClientResponse result = client.post("/mcp").submit(Nodes.newObject())) {
			assertThat(result)
				.hasStatus(401)
				.hasHeader(
					"WWW-Authenticate",
					"Bearer resource_metadata=\"" + API_HOSTNAME + "/.well-known/oauth-protected-resource\""
				);
		}
		verify(handler, never()).handle(any(), any());
	}

	@Test
	public void testFirstPartyTokenForbidden() {
		when(authContext.current(any())).thenReturn(new Authorization(user)); // scope=null, first-party
		try (Http1ClientResponse result = client.post("/mcp").submit(Nodes.newObject())) {
			assertThat(result).hasStatus(403);
		}
		verify(handler, never()).handle(any(), any());
	}

	@Test
	public void testSuspendedExternalTokenForbidden() {
		when(authContext.current(any())).thenReturn(
			new Authorization(user, clientId, Auth0TokenAuthorizer.EXTERNAL_SCOPE)
		);
		when(authContext.userState(user)).thenReturn(UserStateCache.UserState.SUSPENDED);
		try (Http1ClientResponse result = client.post("/mcp").submit(Nodes.newObject())) {
			assertThat(result).hasStatus(403);
		}
		verify(handler, never()).handle(any(), any());
	}

	@Test
	public void testExternalTokenDispatchesToHandler() {
		when(authContext.current(any())).thenReturn(
			new Authorization(user, clientId, Auth0TokenAuthorizer.EXTERNAL_SCOPE)
		);
		when(authContext.userState(user)).thenReturn(UserStateCache.UserState.ACTIVE);
		ObjectNode handlerResponse = Nodes.newObject("jsonrpc", "2.0");
		handlerResponse.put("id", 1);
		handlerResponse.set("result", Nodes.newObject());
		when(handler.handle(any(), any())).thenReturn(handlerResponse);

		ObjectNode request = Nodes.newObject();
		request.put("jsonrpc", "2.0");
		request.put("id", 1);
		request.put("method", "ping");

		try (Http1ClientResponse result = client.post("/mcp").submit(request)) {
			assertThat(result).hasStatus(200);
		}
		verify(handler).handle(any(), any());
		verify(clients).touch(user, clientId, null);
	}

	@Test
	public void testNotificationProducesNoContent() {
		when(authContext.current(any())).thenReturn(
			new Authorization(user, clientId, Auth0TokenAuthorizer.EXTERNAL_SCOPE)
		);
		when(authContext.userState(user)).thenReturn(UserStateCache.UserState.ACTIVE);
		when(handler.handle(any(), any())).thenReturn(null);

		try (Http1ClientResponse result = client.post("/mcp").submit(Nodes.newObject())) {
			assertThat(result).hasStatus(204);
		}
	}

	@Test
	public void testExternalTokenWithoutClientChallenges() {
		when(authContext.current(any())).thenReturn(new Authorization(user, null, Auth0TokenAuthorizer.EXTERNAL_SCOPE));
		when(authContext.userState(user)).thenReturn(UserStateCache.UserState.ACTIVE);

		try (Http1ClientResponse result = client.post("/mcp").submit(Nodes.newObject())) {
			assertThat(result).hasStatus(401);
		}
		verify(handler, never()).handle(any(), any());
	}
}
