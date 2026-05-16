package com.zenobase.mcp;

import com.zenobase.oauth.Authorization;
import io.helidon.common.context.Contexts;
import io.helidon.extensions.mcp.server.McpRequest;

/** Pulls the request's {@link Authorization} out of the Helidon context. Registered by {@link McpAuthFilter}. */
public final class McpAuth {

	private McpAuth() {}

	/**
	 * Returns the {@link Authorization} that the MCP auth filter registered on the current request. Falls back to the
	 * thread's active Helidon context if the McpRequest's request context doesn't have it — defensive against
	 * differences between the filter's request context and the context Helidon MCP propagates to tool callbacks.
	 *
	 * @throws IllegalStateException if no Authorization is present — meaning the filter didn't run or the request
	 *     bypassed it.
	 */
	public static Authorization require(McpRequest request) {
		return request
			.requestContext()
			.get(Authorization.class)
			.or(() -> Contexts.context().flatMap(c -> c.get(Authorization.class)))
			.orElseThrow(() ->
				new IllegalStateException("No Authorization in request context — McpAuthFilter not wired?")
			);
	}
}
