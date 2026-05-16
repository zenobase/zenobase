package com.zenobase.mcp.tools;

import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.CountFacet;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.Schema;
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
		return Schema.builder()
			.rootObject(root ->
				root
					.addStringProperty("bucket_id", p -> p.description("ID of the bucket to query.").required(true))
					.addStringProperty("field", p -> p.description("Field to group by.").required(true))
					.addIntegerProperty("limit", p ->
						p
							.description("Max distinct values to return (1-" + MAX_LIMIT + ", default 10).")
							.minimum(1)
							.maximum(MAX_LIMIT)
					)
					.addArrayProperty("constraints", a ->
						a
							.description("Optional AND-combined predicates ({field, op, value}).")
							.itemsObject(items ->
								items
									.addStringProperty("field", p -> p.description("Field name.").required(true))
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
