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
import com.zenobase.commands.CreateExternalClientCommand;
import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.controllers.ControllerTestSupport;
import com.zenobase.json.Nodes;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.services.CommandDispatcher;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

/**
 * HTTP-level test for the {@code POST /mcp} entry controller — auth gating, RFC 9728 challenge, suspended-user
 * handling, first-observation registration, and successful dispatch into {@link McpJsonRpcHandler}.
 */
public class McpControllerTest extends ControllerTestSupport {

	private static final String API_HOSTNAME = "https://api.zenobase.test";

	private final AuthorizationContext authContext = mock(AuthorizationContext.class);
	private final McpJsonRpcHandler handler = mock(McpJsonRpcHandler.class);
	private final ExternalClientRepository clients = mock(ExternalClientRepository.class);
	private final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
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
				bind(CommandDispatcher.class).toInstance(dispatcher);
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
		verify(dispatcher, never()).dispatch(any());
	}

	@Test
	public void testFirstPartyTokenForbidden() {
		when(authContext.current(any())).thenReturn(new Authorization(user));
		try (Http1ClientResponse result = client.post("/mcp").submit(Nodes.newObject())) {
			assertThat(result).hasStatus(403);
		}
		verify(handler, never()).handle(any(), any());
		verify(dispatcher, never()).dispatch(any());
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
		verify(dispatcher, never()).dispatch(any());
	}

	@Test
	public void testFirstObservationDispatchesCreateCommand() {
		when(authContext.current(any())).thenReturn(
			new Authorization(user, clientId, Auth0TokenAuthorizer.EXTERNAL_SCOPE)
		);
		when(authContext.userState(user)).thenReturn(UserStateCache.UserState.ACTIVE);
		when(clients.find(user, clientId)).thenReturn(null);
		ObjectNode handlerResponse = Nodes.newObject("jsonrpc", "2.0");
		handlerResponse.put("id", 1);
		handlerResponse.set("result", Nodes.newObject());
		when(handler.handle(any(), any())).thenReturn(handlerResponse);

		try (Http1ClientResponse result = client.post("/mcp").submit(ping())) {
			assertThat(result).hasStatus(200);
		}
		verify(dispatcher).dispatch(any(CreateExternalClientCommand.class));
		verify(handler).handle(any(), any());
	}

	@Test
	public void testSubsequentObservationSkipsCommand() {
		when(authContext.current(any())).thenReturn(
			new Authorization(user, clientId, Auth0TokenAuthorizer.EXTERNAL_SCOPE)
		);
		when(authContext.userState(user)).thenReturn(UserStateCache.UserState.ACTIVE);
		when(clients.find(user, clientId)).thenReturn(
			new ExternalClient(user, clientId, null, new DateTime(2026, 5, 1, 0, 0, DateTimeZone.UTC))
		);
		ObjectNode handlerResponse = Nodes.newObject("jsonrpc", "2.0");
		handlerResponse.put("id", 1);
		handlerResponse.set("result", Nodes.newObject());
		when(handler.handle(any(), any())).thenReturn(handlerResponse);

		try (Http1ClientResponse result = client.post("/mcp").submit(ping())) {
			assertThat(result).hasStatus(200);
		}
		verify(dispatcher, never()).dispatch(any());
		verify(handler).handle(any(), any());
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
		verify(dispatcher, never()).dispatch(any());
	}

	private static ObjectNode ping() {
		ObjectNode req = Nodes.newObject();
		req.put("jsonrpc", "2.0");
		req.put("id", 1);
		req.put("method", "ping");
		return req;
	}
}
