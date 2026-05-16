package com.zenobase.mcp.tools;

import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.ListFacet;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.Schema;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Returns raw events from a bucket. Backed by the existing list facet — same code path as the REST API. */
public class EventsTool extends FacetToolSupport {

	private static final int MAX_LIMIT = 500;

	@Inject
	public EventsTool(EventRepository events, ConsentEnforcer enforcer) {
		super(events, enforcer);
	}

	@Override
	public String name() {
		return "events";
	}

	@Override
	public String description() {
		return "Lists raw events from a bucket, optionally filtered by constraints, with limit/offset/order.";
	}

	@Override
	public String schema() {
		return Schema.builder()
			.rootObject(root ->
				root
					.addStringProperty("bucket_id", p ->
						p
							.description("ID of the bucket to query (the {id} from a zenobase://bucket/{id} URI).")
							.required(true)
					)
					.addIntegerProperty("limit", p ->
						p
							.description("Maximum events to return (1-" + MAX_LIMIT + "). Defaults to 50.")
							.minimum(1)
							.maximum(MAX_LIMIT)
					)
					.addIntegerProperty("offset", p ->
						p.description("Pagination offset. Defaults to 0.").minimum(0).maximum(10000)
					)
					.addStringProperty("order", p ->
						p.description(
							"Order expression, e.g. '-timestamp' (most recent first) or 'timestamp' (oldest first). " +
								"Defaults to '-timestamp'."
						)
					)
					.addArrayProperty("constraints", a ->
						a
							.description(
								"Optional list of AND-combined predicates. Each: {field, op, value}. " +
									"Ops: eq, ne, gt, gte, lt, lte, in (value=array), contains (substring). " +
									"Field names come from the bucket's schema (resource zenobase://bucket/{id})."
							)
							.itemsObject(items ->
								items
									.addStringProperty("field", p ->
										p.description("Field name from the bucket schema.").required(true)
									)
									.addStringProperty("op", p -> p.description("Comparison operator.").required(true))
							)
					)
			)
			.build()
			.generate();
	}

	@Override
	public McpToolResult tool(McpToolRequest request) {
		String bucketId = requireString(request, "bucket_id");
		int limit = clamp(optInt(request, "limit", 50), 1, MAX_LIMIT);
		int offset = Math.max(0, optInt(request, "offset", 0));
		String order = optString(request, "order", "-timestamp");
		List<String> constraints = ConstraintParser.parse(request.arguments().get("constraints"));

		Map<String, String> options = new LinkedHashMap<>();
		options.put("id", "events");
		options.put("type", ListFacet.TYPE);
		options.put("offset", Integer.toString(offset));
		options.put("limit", Integer.toString(limit));
		options.put("order", order);
		return runFacet(request, bucketId, constraints, options);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
