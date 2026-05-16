package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.CountFacet;
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
	public ObjectNode inputSchema() {
		Map<String, ObjectNode> extra = new LinkedHashMap<>();
		extra.put("field", ToolSchemas.stringProperty("Field to group by."));
		extra.put(
			"limit",
			ToolSchemas.integerProperty(
				"Max distinct values to return (1-" + MAX_LIMIT + ", default 10).",
				1,
				MAX_LIMIT
			)
		);
		return ToolSchemas.bucketIdAnd(extra, "field");
	}

	@Override
	public JsonNode call(Authorization auth, JsonNode args) {
		String bucketId = ToolArgs.requireString(args, "bucket_id");
		String field = ToolArgs.requireString(args, "field");
		int limit = Math.max(1, Math.min(MAX_LIMIT, ToolArgs.optInt(args, "limit", 10)));
		List<String> constraints = ConstraintParser.parse(args != null ? args.get("constraints") : null);
		Map<String, String> options = new LinkedHashMap<>();
		options.put("id", "terms");
		options.put("type", CountFacet.TYPE);
		options.put("field", field);
		options.put("limit", Integer.toString(limit));
		return runFacet(auth, bucketId, constraints, options);
	}
}
