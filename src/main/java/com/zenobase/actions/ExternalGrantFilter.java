package com.zenobase.actions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.json.Nodes;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.ExternalClientRepository;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jspecify.annotations.Nullable;

/**
 * Enforces per-bucket consent grants on REST routes hit by external-audience tokens. First-party tokens pass through
 * untouched.
 *
 * <p>External tokens may only access a bucket if the client has been granted read access (via
 * {@link ExternalClient#getReadableBuckets()}). Write methods (POST/PUT/DELETE) on bucket-scoped routes are rejected
 * outright until a {@code writable_buckets} grant type lands.
 *
 * <p>The MCP endpoint at {@code POST /mcp} doesn't carry {@code bucketId} in the URL — it's in the JSON-RPC payload —
 * so the filter lets that path through and {@link com.zenobase.mcp.ConsentEnforcer} handles tool-level enforcement.
 *
 * <p>Non-bucket-scoped routes ({@code /users/{userId}/buckets/} etc.) are still rejected for external tokens by the
 * existing {@code auth.getScope() != null → 403} checks in their controllers; this filter doesn't duplicate that.
 */
public class ExternalGrantFilter implements Filter {

	private final AuthorizationContext authContext;
	private final ExternalClientRepository clients;
	private final String webHostname;

	@Inject
	public ExternalGrantFilter(
		AuthorizationContext authContext,
		ExternalClientRepository clients,
		@Named("web.hostname") String webHostname
	) {
		this.authContext = authContext;
		this.clients = clients;
		this.webHostname = webHostname;
	}

	@Override
	public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
		Authorization auth = authContext.current(req);
		if (auth == null || !Auth0TokenAuthorizer.EXTERNAL_SCOPE.equals(auth.getScope())) {
			chain.proceed();
			return;
		}
		String path = req.prologue().uriPath().rawPath();
		String bucketId = bucketIdFrom(path);
		if (bucketId == null) {
			// not a bucket-scoped REST route; MCP / first-party-only routes handle their own gates
			chain.proceed();
			return;
		}
		if (isWrite(req)) {
			sendForbidden(res, "Write access not granted for external clients");
			return;
		}
		Identity clientId = auth.getClient();
		if (clientId == null) {
			sendForbidden(res, "Token has no client identifier");
			return;
		}
		ExternalClient client = clients.find(auth.getPrincipal(), clientId);
		if (client == null || !client.canRead(bucketId)) {
			sendForbidden(res, "This bucket has not been granted to this client");
			return;
		}
		chain.proceed();
	}

	/**
	 * Extracts the {@code bucketId} from a {@code /buckets/{id}/...} path, or returns {@code null} if the path isn't
	 * bucket-scoped. Package-private for testing.
	 */
	static @Nullable String bucketIdFrom(String path) {
		String stripped = path.startsWith("/") ? path.substring(1) : path;
		String[] segments = stripped.split("/", -1);
		if (segments.length < 2 || !"buckets".equals(segments[0])) {
			return null;
		}
		String id = segments[1];
		return id.isEmpty() ? null : id;
	}

	private static boolean isWrite(RoutingRequest req) {
		String method = req.prologue().method().text();
		return !"GET".equals(method) && !"HEAD".equals(method);
	}

	private void sendForbidden(RoutingResponse res, String message) {
		ObjectNode body = Nodes.newObject();
		body.put("message", message);
		body.put("consent_url", webHostname + "/settings/connected-apps");
		res.status(Status.FORBIDDEN_403);
		res.header(HeaderNames.CONTENT_TYPE, "application/json");
		res.send(body.toString());
	}
}
