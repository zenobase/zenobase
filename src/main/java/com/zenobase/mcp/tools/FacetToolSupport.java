package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.McpException;
import com.zenobase.models.Bucket;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Search;
import com.zenobase.search.facets.FacetOptions;
import java.util.List;
import java.util.Map;

/** Shared helper for facet-backed tools — constructs and executes the underlying {@link Search}. */
abstract class FacetToolSupport implements McpTool {

	protected final EventRepository events;
	protected final ConsentEnforcer enforcer;

	protected FacetToolSupport(EventRepository events, ConsentEnforcer enforcer) {
		this.events = events;
		this.enforcer = enforcer;
	}

	protected ObjectNode runFacet(
		Authorization auth,
		String bucketId,
		List<String> constraints,
		Map<String, String> options
	) {
		Bucket bucket = enforcer.requireRead(auth, bucketId);
		try {
			Search search = new EventSearchBuilder()
				.addConstraints(constraints)
				.addFacet(new FacetOptions(options))
				.buildSearch();
			return events.find(bucket.getId(), search);
		} catch (IllegalArgumentException e) {
			throw new McpException(McpException.INVALID_PARAMS, "Invalid query: " + e.getMessage());
		}
	}
}
