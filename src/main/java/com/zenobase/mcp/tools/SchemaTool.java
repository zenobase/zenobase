package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.JsonSchema;
import com.zenobase.json.Nodes;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Returns the inferred field schema for a single bucket, including synthesized sub-fields like
 * {@code timestamp.hour_of_day} and {@code timestamp.day_of_week}. Same payload as the existing
 * {@code resources/read zenobase://bucket/{id}} call — exists as a tool because Claude (and other LLM MCP surfaces)
 * don't autonomously fetch resources, only tools. Without this, the model has no way to discover what fields it can
 * filter on when building queries for {@code events}/{@code histogram}/{@code stats}/{@code terms}/{@code timeline}.
 *
 * <p>Same consent boundary as the other read tools: delegates to {@link ConsentEnforcer#requireRead} which throws if
 * the calling client hasn't been granted access to this bucket.
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
	public ObjectNode inputSchema() {
		Map<String, ObjectNode> extra = new LinkedHashMap<>();
		return bucketIdOnly(extra);
	}

	@Override
	public JsonNode call(Authorization auth, JsonNode args) {
		String bucketId = ToolArgs.requireString(args, "bucket_id");
		Bucket bucket = enforcer.requireRead(auth, bucketId);
		ObjectNode payload = Nodes.newObject();
		payload.put("id", bucket.getId());
		if (bucket.getLabel() != null) {
			payload.put("label", bucket.getLabel());
		}
		if (bucket.getDescription() != null) {
			payload.put("description", bucket.getDescription());
		}
		if (bucket.isArchived()) {
			payload.put("archived", true);
		}
		payload.set("schema", JsonSchema.forFields(events.fields(bucket.getId()), Event.READ_ONLY_FIELDS).toJson());
		return payload;
	}

	/**
	 * Like {@link ToolSchemas#bucketIdAnd} but without the constraints array (this tool only takes a bucket_id).
	 */
	private static ObjectNode bucketIdOnly(Map<String, ObjectNode> extra) {
		ObjectNode schema = Nodes.newObject();
		schema.put("type", "object");
		ObjectNode properties = schema.putObject("properties");
		properties.set(
			"bucket_id",
			ToolSchemas.stringProperty(
				"ID of the bucket whose schema to return (the {id} from a zenobase://bucket/{id} URI)."
			)
		);
		for (Map.Entry<String, ObjectNode> entry : extra.entrySet()) {
			properties.set(entry.getKey(), entry.getValue());
		}
		schema.putArray("required").add("bucket_id");
		return schema;
	}
}
