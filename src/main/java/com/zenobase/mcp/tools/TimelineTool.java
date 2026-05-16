package com.zenobase.mcp.tools;

import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.facets.TimelineFacet;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.Schema;
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
	public String schema() {
		return Schema.builder()
			.rootObject(root ->
				root
					.addStringProperty("bucket_id", p -> p.description("ID of the bucket to query.").required(true))
					.addStringProperty("interval", p ->
						p.description("Time bucket size: hour/day/week/month/quarter/year. Defaults to month.")
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
		String interval = optString(request, "interval", "month");
		List<String> constraints = ConstraintParser.parse(request.arguments().get("constraints"));
		Map<String, String> options = new LinkedHashMap<>();
		options.put("id", "timeline");
		options.put("type", TimelineFacet.TYPE);
		options.put("interval", interval);
		return runFacet(request, bucketId, constraints, options);
	}
}
