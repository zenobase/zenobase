package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.StatsFacet;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Extended statistics (count, min, max, sum, avg, stdev) for one numeric field in a bucket. */
public class StatsTool extends FacetToolSupport {

	@Inject
	public StatsTool(EventRepository events, ConsentEnforcer enforcer) {
		super(events, enforcer);
	}

	@Override
	public String name() {
		return "stats";
	}

	@Override
	public String description() {
		return "Aggregate statistics (count/min/max/sum/avg/stdev) for one numeric field in a bucket.";
	}

	@Override
	public ObjectNode inputSchema() {
		Map<String, ObjectNode> extra = new LinkedHashMap<>();
		extra.put("field", ToolSchemas.stringProperty("Name of the numeric field to aggregate."));
		return ToolSchemas.bucketIdAnd(extra, "field");
	}

	@Override
	public JsonNode call(Authorization auth, JsonNode args) {
		String bucketId = ToolArgs.requireString(args, "bucket_id");
		String field = ToolArgs.requireString(args, "field");
		List<String> constraints = ConstraintParser.parse(args != null ? args.get("constraints") : null);
		Map<String, String> options = new LinkedHashMap<>();
		options.put("id", "stats");
		options.put("type", StatsFacet.TYPE);
		options.put("field", field);
		return runFacet(auth, bucketId, constraints, options);
	}
}
