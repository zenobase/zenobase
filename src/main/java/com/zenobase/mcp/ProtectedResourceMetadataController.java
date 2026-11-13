package com.zenobase.mcp;

import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.auth.auth0.Auth0TokenValidator;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves <a href="https://datatracker.ietf.org/doc/html/rfc9728">RFC 9728</a> OAuth Protected Resource Metadata at
 * {@code /.well-known/oauth-protected-resource}. MCP clients fetch this to discover the OAuth Authorization Server
 * for the {@code /mcp} endpoint.
 *
 * <p>The {@code resource} field is the canonical URL of the MCP endpoint, not the Auth0 API audience identifier.
 * MCP clients validate it against the URL they probed and reject the metadata if they don't match — even when the
 * two strings look superficially similar. The audience that tokens carry (and that our validator checks against)
 * is a separate concern, configured per-tenant in Auth0 and exposed by {@link Auth0TokenValidator#externalAudience()}.
 *
 * <p>Helidon MCP doesn't generate this endpoint — it expects clients to fetch OIDC discovery metadata directly from
 * the issuer instead. We keep this controller because zenobase's MCP discovery flow (used by Claude's connector
 * registration) advertises {@code resource_metadata} explicitly rather than relying on OIDC discovery.
 */
public class ProtectedResourceMetadataController {

	private static final Logger logger = LoggerFactory.getLogger(ProtectedResourceMetadataController.class);

	private final Auth0TokenValidator validator;
	private final String apiHostname;

	@Inject
	public ProtectedResourceMetadataController(
		Auth0TokenValidator validator,
		@Named("api.hostname") String apiHostname
	) {
		this.validator = validator;
		this.apiHostname = apiHostname;
	}

	public void get(ServerRequest req, ServerResponse res) {
		logger.info(
			"GET /.well-known/oauth-protected-resource origin={} ua={}",
			req.headers().value(HeaderNames.ORIGIN).orElse(null),
			req.headers().value(HeaderNames.USER_AGENT).orElse(null)
		);
		if (validator.externalAudience() == null) {
			res.status(Status.NOT_FOUND_404).send();
			return;
		}
		JsonObject body = Json.createObjectBuilder()
			.add("resource", apiHostname + "/mcp")
			.add("authorization_servers", Json.createArrayBuilder().add(validator.issuer()))
			.add("scopes_supported", Json.createArrayBuilder().add(Auth0TokenAuthorizer.EXTERNAL_SCOPE))
			.add("bearer_methods_supported", Json.createArrayBuilder().add("header"))
			.build();
		res.send(body);
	}
}
