package com.zenobase.mcp.tools;

import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.HistogramFacet;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.Schema;
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
		return Schema.builder()
			.rootObject(root ->
				root
					.addStringProperty("bucket_id", p -> p.description("ID of the bucket to query.").required(true))
					.addStringProperty("field", p -> p.description("Numeric field to bin on.").required(true))
					.addStringProperty("interval", p ->
						p.description("Bin width (as a number, e.g. \"10\").").required(true)
					)
					.addArrayProperty("constraints", a ->
						a
							.description(
								"Optional list of AND-combined predicates. Each: {field, op, value}. " +
									"Ops: eq, ne, gt, gte, lt, lte, in (value=array), contains (substring)."
							)
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
