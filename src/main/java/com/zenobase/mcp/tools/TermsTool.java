package com.zenobase.mcp.tools;

import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.CountFacet;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Top values for a string/term field (count per distinct value). */
public class TermsTool extends FacetToolSupport {

	private static final int MAX_LIMIT = 500;

	@Inject
	public TermsTool(EventRepository events, ConsentEnforcer enforcer) {
		super(events, enforcer);
	}

	@Override
	public String name() {
		return "terms";
	}

	@Override
	public String description() {
		return "Top distinct values for a field, with counts. Use for tag/category-style analysis.";
	}

	@Override
	public String schema() {
		Map<String, jakarta.json.JsonObject> extras = new LinkedHashMap<>();
		extras.put("field", ToolSchemas.stringProperty("Field to group by."));
		extras.put(
			"limit",
			ToolSchemas.integerProperty(
				"Max distinct values to return (1-" + MAX_LIMIT + ", default 10).",
				1,
				MAX_LIMIT
			)
		);
		return ToolSchemas.bucketIdAnd(extras, "field");
	}

	@Override
	public McpToolResult tool(McpToolRequest request) {
		String bucketId = requireString(request, "bucket_id");
		String field = requireString(request, "field");
		int limit = Math.max(1, Math.min(MAX_LIMIT, optInt(request, "limit", 10)));
		List<String> constraints = ConstraintParser.parse(request.arguments().get("constraints"));
		Map<String, String> options = new LinkedHashMap<>();
		options.put("id", "terms");
		options.put("type", CountFacet.TYPE);
		options.put("field", field);
		options.put("limit", Integer.toString(limit));
		return runFacet(request, bucketId, constraints, options);
	}
}
