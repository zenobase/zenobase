package com.zenobase.mcp.tools;

import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.ConsentRequiredException;
import com.zenobase.mcp.McpAuth;
import com.zenobase.models.Bucket;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.Search;
import com.zenobase.search.facets.FacetOptions;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.jsonrpc.core.JsonRpcError;
import java.util.List;
import java.util.Map;

/**
 * Shared helper for facet-backed MCP tools — pulls the {@link Authorization} from the request context, applies the
 * shared consent check, builds and executes the underlying {@link Search}, and wraps everything in the standard
 * error-handling envelope (consent failures become {@link McpToolResult} with {@code isError=true}; bad-input failures
 * propagate as JSON-RPC errors).
 */
abstract class FacetToolSupport implements McpTool {

	protected final EventRepository events;
	protected final ConsentEnforcer enforcer;

	protected FacetToolSupport(EventRepository events, ConsentEnforcer enforcer) {
		this.events = events;
		this.enforcer = enforcer;
	}

	/**
	 * Resolves auth + bucket, runs the facet search, returns the raw facet result wrapped as text content. Subclasses
	 * call this from their {@link #tool(McpToolRequest)} after extracting tool-specific parameters.
	 */
	protected McpToolResult runFacet(
		McpToolRequest request,
		String bucketId,
		List<String> constraints,
		Map<String, String> options
	) {
		Authorization auth = McpAuth.require(request);
		try {
			Bucket bucket = enforcer.requireRead(auth, bucketId);
			Search search = new EventSearchBuilder()
				.addConstraints(constraints)
				.addFacet(new FacetOptions(options))
				.buildSearch();
			return McpToolResult.create(events.find(bucket.getId(), search).toString());
		} catch (ConsentRequiredException e) {
			return McpToolResult.builder().error(true).addTextContent(e.getMessage()).build();
		} catch (IllegalArgumentException e) {
			throw new McpException(JsonRpcError.INVALID_PARAMS, "Invalid query: " + e.getMessage());
		}
	}

	/** Convenience: pull a required string parameter or throw INVALID_PARAMS. */
	protected static String requireString(McpToolRequest request, String key) {
		try {
			return request
				.arguments()
				.get(key)
				.asString()
				.orElseThrow(() -> new McpException(JsonRpcError.INVALID_PARAMS, "Missing required parameter: " + key));
		} catch (IllegalStateException e) {
			throw new McpException(JsonRpcError.INVALID_PARAMS, key + " must be a string");
		}
	}

	/** Optional string with default. */
	protected static String optString(McpToolRequest request, String key, String defaultValue) {
		try {
			return request.arguments().get(key).asString().orElse(defaultValue);
		} catch (IllegalStateException e) {
			throw new McpException(JsonRpcError.INVALID_PARAMS, key + " must be a string");
		}
	}

	/** Optional integer with default. */
	protected static int optInt(McpToolRequest request, String key, int defaultValue) {
		try {
			return request.arguments().get(key).asInteger().orElse(defaultValue);
		} catch (IllegalStateException e) {
			throw new McpException(JsonRpcError.INVALID_PARAMS, key + " must be an integer");
		}
	}
}
