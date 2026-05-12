package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.McpException;
import com.zenobase.models.Bucket;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Search;
import com.zenobase.search.facets.FacetOptions;
import com.zenobase.search.facets.ListFacet;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Returns raw events from a bucket. Backed by the existing list facet — same code path as the REST API. */
public class EventsTool implements McpTool {

	private static final int MAX_LIMIT = 500;

	private final EventRepository events;
	private final ConsentEnforcer enforcer;

	@Inject
	public EventsTool(EventRepository events, ConsentEnforcer enforcer) {
		this.events = events;
		this.enforcer = enforcer;
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
	public ObjectNode inputSchema() {
		Map<String, ObjectNode> extra = new LinkedHashMap<>();
		extra.put(
			"limit",
			ToolSchemas.integerProperty("Maximum events to return (1-" + MAX_LIMIT + "). Defaults to 50.", 1, MAX_LIMIT)
		);
		extra.put("offset", ToolSchemas.integerProperty("Pagination offset. Defaults to 0.", 0, 10000));
		extra.put(
			"order",
			ToolSchemas.stringProperty(
				"Order expression, e.g. '-timestamp' (most recent first) or 'timestamp' (oldest first). Defaults to '-timestamp'."
			)
		);
		return ToolSchemas.bucketIdAnd(extra);
	}

	@Override
	public JsonNode call(Authorization auth, JsonNode args) {
		String bucketId = ToolArgs.requireString(args, "bucket_id");
		int limit = clamp(ToolArgs.optInt(args, "limit", 50), 1, MAX_LIMIT);
		int offset = Math.max(0, ToolArgs.optInt(args, "offset", 0));
		String order = ToolArgs.optString(args, "order", "-timestamp");
		List<String> constraints = ConstraintParser.parse(args != null ? args.get("constraints") : null);

		Bucket bucket = enforcer.requireRead(auth, bucketId);

		Map<String, String> options = new LinkedHashMap<>();
		options.put("id", "events");
		options.put("type", ListFacet.TYPE);
		options.put("offset", Integer.toString(offset));
		options.put("limit", Integer.toString(limit));
		options.put("order", order);

		try {
			Search search = new EventSearchBuilder()
				.addConstraints(constraints)
				.addFacet(new FacetOptions(options))
				.buildSearch();
			return events.find(bucket.getId(), search);
		} catch (IllegalArgumentException e) {
			throw new McpException(McpException.INVALID_PARAMS, "Invalid query: " + e.getMessage());
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
