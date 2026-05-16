package com.zenobase.mcp.tools;

import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.StatsFacet;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
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
	public String schema() {
		Map<String, jakarta.json.JsonObject> extras = new LinkedHashMap<>();
		extras.put("field", ToolSchemas.stringProperty("Name of the numeric field to aggregate."));
		return ToolSchemas.bucketIdAnd(extras, "field");
	}

	@Override
	public McpToolResult tool(McpToolRequest request) {
		String bucketId = requireString(request, "bucket_id");
		String field = requireString(request, "field");
		List<String> constraints = ConstraintParser.parse(request.arguments().get("constraints"));
		Map<String, String> options = new LinkedHashMap<>();
		options.put("id", "stats");
		options.put("type", StatsFacet.TYPE);
		options.put("field", field);
		return runFacet(request, bucketId, constraints, options);
	}
}
