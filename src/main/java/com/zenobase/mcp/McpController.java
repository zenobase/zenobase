package com.zenobase.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.auth.auth0.Auth0TokenValidator;
import com.zenobase.commands.CreateExternalClientCommand;
import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.controllers.ControllerSupport;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.services.CommandDispatcher;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helidon-facing entry point for the MCP JSON-RPC endpoint at {@code POST /mcp}. Validates that the request carries
 * an {@code external}-scoped Auth0 token, dispatches a {@link CreateExternalClientCommand} on first observation of a
 * given {@code (user, client_id)} pair, then hands the JSON-RPC request to {@link McpJsonRpcHandler}.
 *
 * <p>Unauthenticated requests get a {@code 401} with the
 * <a href="https://datatracker.ietf.org/doc/html/rfc9728">RFC 9728</a> {@code WWW-Authenticate} challenge header so
 * MCP clients can discover the OAuth Authorization Server via
 * {@code /.well-known/oauth-protected-resource}.
 */
public class McpController extends ControllerSupport {

	private static final Logger logger = LoggerFactory.getLogger(McpController.class);

	private final McpJsonRpcHandler handler;
	private final ExternalClientRepository clients;
	private final CommandDispatcher dispatcher;
	private final Auth0TokenValidator validator;
	private final String apiHostname;

	@Inject
	public McpController(
		AuthorizationContext authContext,
		McpJsonRpcHandler handler,
		ExternalClientRepository clients,
		CommandDispatcher dispatcher,
		Auth0TokenValidator validator,
		@Named("api.hostname") String apiHostname
	) {
		super(authContext);
		this.handler = handler;
		this.clients = clients;
		this.dispatcher = dispatcher;
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
		registerIfNew(auth.getPrincipal(), client);
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

	/**
	 * If we haven't seen this {@code (user, client)} pair before, dispatch {@link CreateExternalClientCommand} to
	 * record the first observation. Non-fatal — a failure here doesn't block the request.
	 */
	private void registerIfNew(Identity user, Identity client) {
		try {
			if (clients.find(user, client) == null) {
				ExternalClient record = new ExternalClient(user, client, null, DateTime.now(DateTimeZone.UTC));
				dispatcher.dispatch(new CreateExternalClientCommand(user, record));
			}
		} catch (RuntimeException e) {
			logger.warn("Couldn't register external client {} for user {}", client, user, e);
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
