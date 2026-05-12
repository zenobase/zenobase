package com.zenobase.mcp;

import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.ExternalBucketGrant;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.ExternalBucketGrantRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Single place that decides whether an authenticated MCP request may touch a given bucket. Combines two checks:
 *
 * <ol>
 *   <li>Zenobase's existing role check ({@code bucket.hasRole(auth, Role.VIEWER)}) — same as REST controllers.</li>
 *   <li>An active grant in {@link ExternalBucketGrantRepository} for {@code (user, client_id, bucket_id, "read")} —
 *       the per-bucket consent the user issues via the Connected Apps page.</li>
 * </ol>
 *
 * Anything not satisfying both raises an {@link McpException} that the JSON-RPC layer serializes appropriately.
 */
public class ConsentEnforcer {

	private final BucketRepository buckets;
	private final ExternalBucketGrantRepository grants;
	private final String webHostname;

	@Inject
	public ConsentEnforcer(
		BucketRepository buckets,
		ExternalBucketGrantRepository grants,
		@Named("web.hostname") String webHostname
	) {
		this.buckets = buckets;
		this.grants = grants;
		this.webHostname = webHostname;
	}

	/**
	 * Loads the bucket, asserts the authorized user can view it, and asserts the requesting external client has an
	 * active read grant. Returns the loaded {@link Bucket} for the caller to use. Throws {@link McpException} with
	 * {@link McpException#ACCESS_NOT_GRANTED} (carrying a consent URL) or
	 * {@link McpException#INVALID_PARAMS} (bucket not found / wrong owner).
	 */
	public Bucket requireRead(Authorization auth, String bucketId) {
		Identity client = auth.getClient();
		if (client == null) {
			throw new McpException(McpException.ACCESS_NOT_GRANTED, "Token has no client identifier", consentData());
		}
		Bucket bucket = buckets.find(bucketId);
		if (bucket == null) {
			throw new McpException(McpException.INVALID_PARAMS, "Bucket not found: " + bucketId);
		}
		if (!bucket.hasRole(auth, Role.VIEWER)) {
			throw new McpException(McpException.ACCESS_NOT_GRANTED, "No access to bucket: " + bucketId, consentData());
		}
		ExternalBucketGrant grant = grants.find(auth.getPrincipal(), client, bucketId);
		if (grant == null) {
			throw new McpException(
				McpException.ACCESS_NOT_GRANTED,
				"This bucket has not been granted to this client",
				consentData()
			);
		}
		if (!ExternalBucketGrant.RIGHT_READ.equals(grant.getRights())) {
			throw new McpException(
				McpException.ACCESS_NOT_GRANTED,
				"This client does not have read access to this bucket",
				consentData()
			);
		}
		return bucket;
	}

	public String consentUrl() {
		return webHostname + "/settings/connected-apps";
	}

	private com.fasterxml.jackson.databind.node.ObjectNode consentData() {
		return Nodes.newObject("consent_url", consentUrl());
	}
}
