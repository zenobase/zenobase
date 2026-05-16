package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.HistogramFacet;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Numeric histogram (bucket counts) for one numeric field in a bucket. */
public class HistogramTool extends FacetToolSupport {

	@Inject
	public HistogramTool(EventRepository events, ConsentEnforcer enforcer) {
		super(events, enforcer);
	}

	@Override
	public String name() {
		return "histogram";
	}

	@Override
	public String description() {
		return "Counts of events binned by a numeric field, with a configurable interval.";
	}

	@Override
	public ObjectNode inputSchema() {
		Map<String, ObjectNode> extra = new LinkedHashMap<>();
		extra.put("field", ToolSchemas.stringProperty("Numeric field to bin on."));
		extra.put("interval", ToolSchemas.stringProperty("Bin width (as a number, e.g. \"10\")."));
		return ToolSchemas.bucketIdAnd(extra, "field", "interval");
	}

	@Override
	public JsonNode call(Authorization auth, JsonNode args) {
		String bucketId = ToolArgs.requireString(args, "bucket_id");
		String field = ToolArgs.requireString(args, "field");
		String interval = ToolArgs.requireString(args, "interval");
		List<String> constraints = ConstraintParser.parse(args != null ? args.get("constraints") : null);
		Map<String, String> options = new LinkedHashMap<>();
		options.put("id", "histogram");
		options.put("type", HistogramFacet.TYPE);
		options.put("field", field);
		options.put("interval", interval);
		return runFacet(auth, bucketId, constraints, options);
	}
}
