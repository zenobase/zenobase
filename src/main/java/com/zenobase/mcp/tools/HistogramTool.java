package com.zenobase.mcp.tools;

import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.HistogramFacet;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
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
	public String schema() {
		Map<String, jakarta.json.JsonObject> extras = new LinkedHashMap<>();
		extras.put("field", ToolSchemas.stringProperty("Numeric field to bin on."));
		extras.put("interval", ToolSchemas.stringProperty("Bin width (as a number, e.g. \"10\")."));
		return ToolSchemas.bucketIdAnd(extras, "field", "interval");
	}

	@Override
	public McpToolResult tool(McpToolRequest request) {
		String bucketId = requireString(request, "bucket_id");
		String field = requireString(request, "field");
		String interval = requireString(request, "interval");
		List<String> constraints = ConstraintParser.parse(request.arguments().get("constraints"));
		Map<String, String> options = new LinkedHashMap<>();
		options.put("id", "histogram");
		options.put("type", HistogramFacet.TYPE);
		options.put("field", field);
		options.put("interval", interval);
		return runFacet(request, bucketId, constraints, options);
	}
}
