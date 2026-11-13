package com.zenobase.mcp.tools;

import com.zenobase.json.JsonSchema;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.ConsentRequiredException;
import com.zenobase.mcp.JsonSchemas;
import com.zenobase.mcp.McpAuth;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.jsonrpc.core.JsonRpcError;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

/**
 * Returns the inferred field schema for a single bucket, including synthesized sub-fields like
 * {@code timestamp.hour_of_day} and {@code timestamp.day_of_week}. Same payload as the {@code resources/read
 * zenobase://bucket/{id}} call — exists as a tool because Claude (and other LLM MCP surfaces) don't autonomously fetch
 * resources, only tools. Without this, the model has no way to discover what fields it can filter on when building
 * queries for {@code events}/{@code histogram}/{@code stats}/{@code terms}/{@code timeline}.
 */
public class SchemaTool implements McpTool {

	private final EventRepository events;
	private final ConsentEnforcer enforcer;

	@Inject
	public SchemaTool(EventRepository events, ConsentEnforcer enforcer) {
		this.events = events;
		this.enforcer = enforcer;
	}

	@Override
	public String name() {
		return "schema";
	}

	@Override
	public String description() {
		return (
			"Returns the schema for one bucket: id, label, description, archived flag, and the JSON Schema of its " +
			"fields. Timestamp-typed fields expose synthesized sub-fields (e.g. timestamp.hour_of_day, " +
			"timestamp.day_of_week, timestamp.month_of_year) that can be used in constraints to filter by time of day, " +
			"day of week, etc. Call this when you need to know what fields are queryable on a specific bucket before " +
			"building constraints for events/histogram/stats/terms/timeline."
		);
	}

	@Override
	public String schema() {
		return ToolSchemas.bucketIdOnly();
	}

	@Override
	public McpToolResult tool(McpToolRequest request) {
		Authorization auth = McpAuth.require(request);
		String bucketId = request
			.arguments()
			.get("bucket_id")
			.asString()
			.orElseThrow(() -> new McpException(JsonRpcError.INVALID_PARAMS, "Missing required parameter: bucket_id"));
		try {
			Bucket bucket = enforcer.requireRead(auth, bucketId);
			JsonObjectBuilder payload = Json.createObjectBuilder().add("id", bucket.getId());
			if (bucket.getLabel() != null) {
				payload.add("label", bucket.getLabel());
			}
			if (bucket.getDescription() != null) {
				payload.add("description", bucket.getDescription());
			}
			if (bucket.isArchived()) {
				payload.add("archived", true);
			}
			payload.add(
				"schema",
				JsonSchemas.toJsonObject(JsonSchema.forFields(events.fields(bucket.getId()), Event.READ_ONLY_FIELDS))
			);
			return McpToolResult.create(payload.build().toString());
		} catch (ConsentRequiredException e) {
			return McpToolResult.builder().error(true).addTextContent(e.getMessage()).build();
		}
	}
}
