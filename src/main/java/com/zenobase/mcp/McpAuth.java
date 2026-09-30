package com.zenobase.mcp;

import com.zenobase.oauth.Authorization;
import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.extensions.mcp.server.McpRequest;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pulls the request's {@link Authorization} out of the Helidon context. Registered by {@link McpAuthFilter}, which
 * runs on every {@code /mcp} request and stashes the validated {@link Authorization} on {@code req.context()} (which
 * Helidon MCP exposes as {@code McpRequest.requestContext()}).
 *
 * <p>Three lookups in order: {@code request.requestContext()} (canonical, per
 * {@code helidon4-extensions-mcp-server} source), then thread-active {@link Contexts#context()} (defensive — the
 * filter wraps {@code chain.proceed()} in {@code Contexts.runInContext} so this is the same Context), then the
 * session context (last resort, e.g. if a future change moves auth to the session level).
 */
public final class McpAuth {

	private static final Logger logger = LoggerFactory.getLogger(McpAuth.class);

	private McpAuth() {}

	public static Authorization require(McpRequest request) {
		Context requestCtx = request.requestContext();
		Optional<Authorization> fromRequest = requestCtx.get(Authorization.class);
		if (fromRequest.isPresent()) {
			return fromRequest.get();
		}
		Optional<Authorization> fromThread = Contexts.context().flatMap(c -> c.get(Authorization.class));
		if (fromThread.isPresent()) {
			logger.warn(
				"Authorization missing from McpRequest.requestContext() but found in thread-active Contexts.context() —" +
					" the two Context instances are diverging."
			);
			return fromThread.get();
		}
		Optional<Authorization> fromSession = request.sessionContext().get(Authorization.class);
		if (fromSession.isPresent()) {
			return fromSession.get();
		}
		logger.error(
			"No Authorization in any context (request, thread, session). McpAuthFilter likely didn't run on this " +
				"request — check filter logs for the matching path."
		);
		throw new IllegalStateException("No Authorization in request context — McpAuthFilter not wired?");
	}
}
