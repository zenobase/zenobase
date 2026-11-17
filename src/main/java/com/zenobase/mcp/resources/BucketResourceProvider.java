package com.zenobase.mcp.resources;

import com.zenobase.json.JsonSchema;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.ConsentRequiredException;
import com.zenobase.mcp.JsonSchemas;
import com.zenobase.mcp.McpAuth;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpResource;
import io.helidon.extensions.mcp.server.McpResourceRequest;
import io.helidon.extensions.mcp.server.McpResourceResult;
import io.helidon.jsonrpc.core.JsonRpcError;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import java.util.Optional;

/**
 * Exposes the user's granted Zenobase buckets as MCP Resources. URI template is
 * {@code zenobase://bucket/{bucket_id}} — a single resource template covers every bucket; the {@code bucket_id} comes
 * out of {@link McpResourceRequest#parameters()} via Helidon's RFC 6570 template matching.
 *
 * <p>Note: {@code resources/list} discovery uses the {@link com.zenobase.mcp.tools.BucketsTool} surface instead.
 * Helidon-MCP only enumerates concrete (non-templated) resources in {@code resources/list}; templated resources are
 * resolved on-demand via {@code resources/read}. We don't list one resource per bucket because the granted-bucket set
 * is per-client and needs the {@link Authorization} to compute, which the resource-list path doesn't expose.
 */
public class BucketResourceProvider implements McpResource {

	public static final String URI_PREFIX = "zenobase://bucket/";
	public static final String URI_TEMPLATE = URI_PREFIX + "{bucket_id}";

	private final EventRepository events;
	private final ConsentEnforcer enforcer;

	@Inject
	public BucketResourceProvider(EventRepository events, ConsentEnforcer enforcer) {
		this.events = events;
		this.enforcer = enforcer;
	}

	@Override
	public String uri() {
		return URI_TEMPLATE;
	}

	@Override
	public String name() {
		return "bucket";
	}

	@Override
	public String description() {
		return "A Zenobase bucket: id, label, description, archived flag, and the JSON Schema of its fields.";
	}

	@Override
	public Optional<String> title() {
		return Optional.of("Zenobase Bucket");
	}

	@Override
	public MediaType mediaType() {
		return MediaTypes.APPLICATION_JSON;
	}

	@Override
	public McpResourceResult resource(McpResourceRequest request) {
		Authorization auth = McpAuth.require(request);
		String bucketId = request
			.parameters()
			.get("bucket_id")
			.asString()
			.orElseThrow(() -> new McpException(JsonRpcError.INVALID_PARAMS, "Missing bucket_id in URI"));
		try {
			Bucket bucket = enforcer.requireRead(auth, bucketId);
			JsonObjectBuilder payload = Json.createObjectBuilder().add("@id", bucket.getId());
			if (bucket.getLabel() != null) {
				payload.add("label", bucket.getLabel());
			}
			if (bucket.getDescription() != null) {
				payload.add("description", bucket.getDescription());
			}
			payload.add("archived", bucket.isArchived());
			payload.add(
				"schema",
				JsonSchemas.toJsonObject(JsonSchema.forFields(events.fields(bucket.getId()), Event.READ_ONLY_FIELDS))
			);
			return McpResourceResult.create(payload.build().toString());
		} catch (ConsentRequiredException e) {
			// -32002 mirrors the application-defined "access not granted" code our previous JSON-RPC handler used and
			// matches MCP convention for non-protocol auth failures. INVALID_PARAMS would imply a malformed URI, which
			// would mislead the client into re-formatting its request rather than prompting the user to grant access.
			throw new McpException(-32002, e.getMessage());
		}
	}
}
