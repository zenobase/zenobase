package com.zenobase.mcp;

import com.zenobase.auth.IdentityProvider;
import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.auth.auth0.Auth0TokenValidator;
import com.zenobase.commands.CreateExternalClientCommand;
import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.services.CommandDispatcher;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gates the {@code /mcp} endpoint(s) so the downstream {@link io.helidon.extensions.mcp.server.McpServerFeature} only
 * sees fully-authenticated requests, and surfaces our {@link Authorization} object to tools/resources via the request
 * {@link io.helidon.common.context.Context}.
 *
 * <p>Replaces the explicit auth flow that used to live in {@code McpController.post(...)}. Same semantics:
 *
 * <ul>
 *   <li>No token / non-JWT / unknown issuer → {@code 401} with the RFC 9728 {@code WWW-Authenticate: Bearer
 *       resource_metadata=...} challenge so MCP clients can discover the Authorization Server.</li>
 *   <li>Token without the {@code external} scope → {@code 403}.</li>
 *   <li>Suspended user → {@code 403}.</li>
 *   <li>Token missing a client identifier → {@code 401} (same challenge).</li>
 *   <li>First observation of a {@code (user, client)} pair → dispatches a {@link CreateExternalClientCommand}. Failure
 *       here is logged but does not block the request.</li>
 *   <li>On success: registers {@link Authorization} in {@code req.context()} for tool/resource handlers and proceeds.</li>
 * </ul>
 *
 * <p>Wired in {@link com.zenobase.Routing} only for the MCP path so the first-party REST API is unaffected.
 */
public class McpAuthFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(McpAuthFilter.class);

	private final AuthorizationContext authContext;
	private final ExternalClientRepository clients;
	private final CommandDispatcher dispatcher;
	private final Auth0TokenValidator validator;
	private final IdentityProvider identityProvider;
	private final String apiHostname;

	@Inject
	public McpAuthFilter(
		AuthorizationContext authContext,
		ExternalClientRepository clients,
		CommandDispatcher dispatcher,
		Auth0TokenValidator validator,
		IdentityProvider identityProvider,
		@Named("api.hostname") String apiHostname
	) {
		this.authContext = authContext;
		this.clients = clients;
		this.dispatcher = dispatcher;
		this.validator = validator;
		this.identityProvider = identityProvider;
		this.apiHostname = apiHostname;
	}

	@Override
	public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
		// Helidon's `routing.addFilter(...)` registers globally — there is no path-scoped variant. Guard explicitly so
		// this filter only runs on the MCP endpoint(s) and doesn't reject every first-party REST request.
		String path = req.prologue().uriPath().rawPath();
		if (!path.equals("/mcp") && !path.startsWith("/mcp/")) {
			chain.proceed();
			return;
		}
		Authorization auth = authContext.current(req);
		logger.info(
			"{} {} origin={} ua={} authenticated={} scope={}",
			req.prologue().method().text(),
			path,
			req.headers().value(HeaderNames.ORIGIN).orElse(null),
			req.headers().value(HeaderNames.USER_AGENT).orElse(null),
			auth != null,
			auth != null ? auth.getScope() : null
		);
		if (auth == null) {
			challenge(res);
			return;
		}
		if (!Auth0TokenAuthorizer.EXTERNAL_SCOPE.equals(auth.getScope())) {
			res.status(Status.FORBIDDEN_403).send();
			return;
		}
		if (authContext.userState(auth.getPrincipal()) == com.zenobase.auth.UserStateCache.UserState.SUSPENDED) {
			res.status(Status.FORBIDDEN_403).send();
			return;
		}
		Identity client = auth.getClient();
		if (client == null) {
			challenge(res);
			return;
		}
		registerIfNew(auth.getPrincipal(), client);
		req.context().register(Authorization.class, auth);
		chain.proceed();
	}

	/**
	 * If we haven't seen this {@code (user, client)} pair before, dispatch {@link CreateExternalClientCommand} to record
	 * the first observation. Non-fatal — a failure here doesn't block the request.
	 */
	private void registerIfNew(Identity user, Identity client) {
		try {
			if (clients.find(user, client) == null) {
				String name = identityProvider.getApplicationName(client);
				ExternalClient record = new ExternalClient(user, client, name, DateTime.now(DateTimeZone.UTC));
				dispatcher.dispatch(new CreateExternalClientCommand(user, record));
			}
		} catch (RuntimeException e) {
			logger.warn("Couldn't register external client {} for user {}", client, user, e);
		}
	}

	/** Sends a 401 with the RFC 9728 challenge header pointing at our protected-resource metadata. */
	private void challenge(RoutingResponse res) {
		if (validator.externalAudience() != null) {
			String metadataUrl = apiHostname + "/.well-known/oauth-protected-resource";
			res.header(HeaderNames.WWW_AUTHENTICATE, "Bearer resource_metadata=\"" + metadataUrl + "\"");
		}
		res.status(Status.UNAUTHORIZED_401).send();
	}
}
