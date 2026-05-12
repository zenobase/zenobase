package com.zenobase.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.auth.auth0.Auth0TokenValidator;
import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.controllers.ControllerSupport;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.ExternalClientRepository;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Helidon-facing entry point for the MCP JSON-RPC endpoint at {@code POST /mcp}. Validates that the request carries
 * an {@code external}-scoped Auth0 token, opportunistically touches the {@code external_clients} record, then hands
 * the JSON-RPC request to {@link McpJsonRpcHandler}.
 *
 * <p>Unauthenticated requests get a {@code 401} with the
 * <a href="https://datatracker.ietf.org/doc/html/rfc9728">RFC 9728</a> {@code WWW-Authenticate} challenge header so
 * MCP clients can discover the OAuth Authorization Server via
 * {@code /.well-known/oauth-protected-resource}.
 */
public class McpController extends ControllerSupport {

	private final McpJsonRpcHandler handler;
	private final ExternalClientRepository clients;
	private final Auth0TokenValidator validator;
	private final String apiHostname;

	@Inject
	public McpController(
		AuthorizationContext authContext,
		McpJsonRpcHandler handler,
		ExternalClientRepository clients,
		Auth0TokenValidator validator,
		@Named("api.hostname") String apiHostname
	) {
		super(authContext);
		this.handler = handler;
		this.clients = clients;
		this.validator = validator;
		this.apiHostname = apiHostname;
	}

	public void post(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			challenge(res);
			return;
		}
		if (!Auth0TokenAuthorizer.EXTERNAL_SCOPE.equals(auth.getScope())) {
			res.status(Status.FORBIDDEN_403);
			res.send();
			return;
		}
		if (sendForbiddenIfSuspended(auth, res)) {
			return;
		}
		Identity client = auth.getClient();
		if (client == null) {
			challenge(res);
			return;
		}
		// Opportunistic touch — non-fatal if it fails.
		try {
			clients.touch(auth.getPrincipal(), client, null);
		} catch (RuntimeException e) {
			// best-effort; logging happens at the index layer
		}
		JsonNode request;
		try {
			request = req.content().as(JsonNode.class);
		} catch (Exception e) {
			sendBadRequest(res, "Malformed JSON");
			return;
		}
		if (request == null) {
			sendBadRequest(res, "Missing JSON body");
			return;
		}
		ObjectNode response = handler.handle(auth, request);
		if (response == null) {
			res.status(Status.NO_CONTENT_204).send();
		} else {
			sendOk(res, response);
		}
	}

	/** Sends a 401 with the RFC 9728 challenge header pointing at our protected-resource metadata. */
	private void challenge(ServerResponse res) {
		if (validator.externalAudience() != null) {
			String metadataUrl = apiHostname + "/.well-known/oauth-protected-resource";
			res.header(HeaderNames.WWW_AUTHENTICATE, "Bearer resource_metadata=\"" + metadataUrl + "\"");
		}
		res.status(Status.UNAUTHORIZED_401).send();
	}
}
