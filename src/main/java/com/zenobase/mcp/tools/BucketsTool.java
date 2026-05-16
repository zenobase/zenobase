package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.Nodes;
import com.zenobase.mcp.GrantedBuckets;
import com.zenobase.mcp.McpAuth;
import com.zenobase.models.Bucket;
import com.zenobase.oauth.Authorization;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.Schema;
import jakarta.inject.Inject;

/**
 * Lists the buckets the calling external client has been granted access to. Exists alongside the {@code resources/list}
 * provider in {@link com.zenobase.mcp.resources.BucketResourceProvider} because Claude (and likely other LLM MCP
 * surfaces) do not autonomously invoke {@code resources/list} from conversational context — they treat resources as
 * user-attached items rather than model-discoverable catalogs. Exposing the same data as a tool means the model can
 * call it when the user says something like "use Zenobase" without first knowing a {@code bucket_id}.
 */
public class BucketsTool implements McpTool {

	private final GrantedBuckets granted;

	@Inject
	public BucketsTool(GrantedBuckets granted) {
		this.granted = granted;
	}

	@Override
	public String name() {
		return "buckets";
	}

	@Override
	public String description() {
		return (
			"Lists the Zenobase buckets the user has granted this client access to. Returns id, label, description, " +
			"and archived flag for each. Call this first when the user references Zenobase data without specifying a " +
			"particular bucket, so subsequent calls to events/histogram/stats/terms/timeline can pass the right bucket_id. " +
			"To see which fields each bucket has (including synthesized timestamp sub-fields usable in constraints), " +
			"call the `schema` tool with the bucket's id."
		);
	}

	@Override
	public String schema() {
		return Schema.builder()
			.rootObject(root -> {})
			.build()
			.generate();
	}

	@Override
	public McpToolResult tool(McpToolRequest request) {
		Authorization auth = McpAuth.require(request);
		GrantedBuckets.Result result = granted.list(auth);
		ObjectNode node = Nodes.newObject();
		ArrayNode array = node.putArray("buckets");
		for (Bucket bucket : result.buckets()) {
			array.add(toJson(bucket));
		}
		if (result.consentUrl() != null) {
			node.putObject("_meta").put("consent_url", result.consentUrl());
		}
		return McpToolResult.create(node.toString());
	}

	private static ObjectNode toJson(Bucket bucket) {
		ObjectNode node = Nodes.newObject();
		node.put("id", bucket.getId());
		if (bucket.getLabel() != null) {
			node.put("label", bucket.getLabel());
		}
		if (bucket.getDescription() != null) {
			node.put("description", bucket.getDescription());
		}
		if (bucket.isArchived()) {
			node.put("archived", true);
		}
		return node;
	}
}
