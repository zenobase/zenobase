package com.zenobase.mcp;

import com.zenobase.models.Bucket;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.ExternalClientRepository;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.jsonrpc.core.JsonRpcError;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Single place that decides whether an authenticated MCP request may touch a given bucket. Combines two checks:
 *
 * <ol>
 *   <li>Zenobase's existing role check ({@code bucket.hasRole(auth, Role.VIEWER)}) — same as REST controllers.</li>
 *   <li>An entry in the requesting client's {@code readable_buckets} list — the per-bucket consent the user issues via
 *       the API clients section of Settings.</li>
 * </ol>
 *
 * <p>Failure modes:
 * <ul>
 *   <li>Bucket missing → {@link McpException} with {@link JsonRpcError#INVALID_PARAMS} (surfaces as a JSON-RPC error,
 *       since this is a protocol-level "bad input").</li>
 *   <li>Role denied / grant missing / client missing → {@link ConsentRequiredException} (tools turn this into an
 *       {@code McpToolResult} with {@code isError=true} so the model can prompt the user).</li>
 * </ul>
 */
public class ConsentEnforcer {

	private final BucketRepository buckets;
	private final ExternalClientRepository clients;
	private final String webHostname;

	@Inject
	public ConsentEnforcer(
		BucketRepository buckets,
		ExternalClientRepository clients,
		@Named("web.hostname") String webHostname
	) {
		this.buckets = buckets;
		this.clients = clients;
		this.webHostname = webHostname;
	}

	/**
	 * Loads the bucket, asserts the authorized user can view it, and asserts the requesting external client has been
	 * granted read access to it. Returns the loaded {@link Bucket} for the caller to use.
	 */
	public Bucket requireRead(Authorization auth, String bucketId) {
		Identity clientId = auth.getClient();
		if (clientId == null) {
			throw new ConsentRequiredException(
				"Token has no client identifier. Consent URL: " + consentUrl(),
				consentUrl()
			);
		}
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			throw new McpException(JsonRpcError.INVALID_PARAMS, "Bucket not found: " + bucketId);
		}
		if (!bucket.hasRole(auth, Role.VIEWER)) {
			throw new ConsentRequiredException(
				"No access to bucket: " + bucketId + ". Consent URL: " + consentUrl(),
				consentUrl()
			);
		}
		ExternalClient client = clients.find(auth.getPrincipal(), clientId);
		if (client == null || !client.canRead(bucketId)) {
			throw new ConsentRequiredException(
				"This bucket has not been granted to this client. Consent URL: " + consentUrl(),
				consentUrl()
			);
		}
		return bucket;
	}

	public String consentUrl() {
		return webHostname + "/#/settings";
	}
}
