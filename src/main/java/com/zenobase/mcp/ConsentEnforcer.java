package com.zenobase.mcp;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.ExternalClientRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Single place that decides whether an authenticated MCP request may touch a given bucket. Combines two checks:
 *
 * <ol>
 *   <li>Zenobase's existing role check ({@code bucket.hasRole(auth, Role.VIEWER)}) — same as REST controllers.</li>
 *   <li>An entry in the requesting client's {@code readable_buckets} list — the per-bucket consent the user issues via
 *       the Connected Apps page.</li>
 * </ol>
 *
 * Anything not satisfying both raises an {@link McpException} that the JSON-RPC layer serializes appropriately.
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
	 * granted read access to it. Returns the loaded {@link Bucket} for the caller to use. Throws {@link McpException}
	 * with {@link McpException#ACCESS_NOT_GRANTED} (carrying a consent URL) or {@link McpException#INVALID_PARAMS}
	 * (bucket not found / wrong owner).
	 */
	public Bucket requireRead(Authorization auth, String bucketId) {
		Identity clientId = auth.getClient();
		if (clientId == null) {
			throw new McpException(McpException.ACCESS_NOT_GRANTED, "Token has no client identifier", consentData());
		}
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			throw new McpException(McpException.INVALID_PARAMS, "Bucket not found: " + bucketId);
		}
		if (!bucket.hasRole(auth, Role.VIEWER)) {
			throw new McpException(McpException.ACCESS_NOT_GRANTED, "No access to bucket: " + bucketId, consentData());
		}
		ExternalClient client = clients.find(auth.getPrincipal(), clientId);
		if (client == null || !client.canRead(bucketId)) {
			throw new McpException(
				McpException.ACCESS_NOT_GRANTED,
				"This bucket has not been granted to this client",
				consentData()
			);
		}
		return bucket;
	}

	public String consentUrl() {
		return webHostname + "/#/settings";
	}

	private com.fasterxml.jackson.databind.node.ObjectNode consentData() {
		return Nodes.newObject("consent_url", consentUrl());
	}
}
