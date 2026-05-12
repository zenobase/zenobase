package com.zenobase.mcp;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.auth.auth0.Auth0TokenValidator;
import com.zenobase.json.Nodes;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;

/**
 * Serves <a href="https://datatracker.ietf.org/doc/html/rfc9728">RFC 9728</a> OAuth Protected Resource Metadata at
 * {@code /.well-known/oauth-protected-resource}. MCP clients fetch this to discover the OAuth Authorization Server
 * for the {@code /mcp} endpoint.
 */
public class ProtectedResourceMetadataController {

	private final Auth0TokenValidator validator;

	@Inject
	public ProtectedResourceMetadataController(Auth0TokenValidator validator) {
		this.validator = validator;
	}

	public void get(ServerRequest req, ServerResponse res) {
		String externalAudience = validator.externalAudience();
		if (externalAudience == null) {
			res.status(Status.NOT_FOUND_404).send();
			return;
		}
		ObjectNode node = Nodes.newObject();
		node.put("resource", externalAudience);
		ArrayNode authServers = node.putArray("authorization_servers");
		authServers.add(validator.issuer());
		ArrayNode scopes = node.putArray("scopes_supported");
		scopes.add(Auth0TokenAuthorizer.EXTERNAL_SCOPE);
		ArrayNode methods = node.putArray("bearer_methods_supported");
		methods.add("header");
		res.send(node);
	}
}
