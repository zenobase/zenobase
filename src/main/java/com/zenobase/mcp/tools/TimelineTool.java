package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.TimelineFacet;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Time-bucketed counts (and optional sums) over a date range. */
public class TimelineTool extends FacetToolSupport {

	@Inject
	public TimelineTool(EventRepository events, ConsentEnforcer enforcer) {
		super(events, enforcer);
	}

	@Override
	public String name() {
		return "timeline";
	}

	@Override
	public String description() {
		return "Counts events bucketed in time (by day/week/month/etc).";
	}

	@Override
	public ObjectNode inputSchema() {
		Map<String, ObjectNode> extra = new LinkedHashMap<>();
		extra.put(
			"interval",
			ToolSchemas.enumProperty(
				"Time bucket size. Defaults to month.",
				"hour",
				"day",
				"week",
				"month",
				"quarter",
				"year"
			)
		);
		return ToolSchemas.bucketIdAnd(extra);
	}

	@Override
	public JsonNode call(Authorization auth, JsonNode args) {
		String bucketId = ToolArgs.requireString(args, "bucket_id");
		String interval = ToolArgs.optString(args, "interval", "month");
		List<String> constraints = ConstraintParser.parse(args != null ? args.get("constraints") : null);
		Map<String, String> options = new LinkedHashMap<>();
		options.put("id", "timeline");
		options.put("type", TimelineFacet.TYPE);
		options.put("interval", interval);
		return runFacet(auth, bucketId, constraints, options);
	}
}
