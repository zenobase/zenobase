package io.helidon.extensions.mcp.server;

import io.helidon.jsonrpc.core.JsonRpcParams;
import jakarta.json.JsonValue;

/**
 * Test-only bridge that exposes {@link McpParameters}'s package-private constructor so unit tests can build a
 * parameter tree without standing up the full Helidon MCP transport. Lives in {@code io.helidon.extensions.mcp.server}
 * so it can see the package-private API.
 */
public final class McpParametersTestAccess {

	private final JsonRpcParams params;
	private final JsonValue root;

	public McpParametersTestAccess(JsonRpcParams params, JsonValue root) {
		this.params = params;
		this.root = root;
	}

	public McpParameters build() {
		return new McpParameters(params, root);
	}
}
