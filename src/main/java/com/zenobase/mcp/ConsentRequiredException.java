package com.zenobase.mcp;

/**
 * Thrown when a client passes auth but isn't permitted to read the requested bucket. Tools catch this and turn it into
 * an {@link io.helidon.extensions.mcp.server.McpToolResult} with {@code isError=true} and a text-content message that
 * embeds the {@link #consentUrl()} so the model can guide the user to grant access.
 *
 * <p>Per the MCP spec, tool-execution errors live in the result envelope (not JSON-RPC errors), and the model is
 * expected to surface them to the user — so the consent URL travels as part of the error text rather than as
 * structured error data (which {@code McpToolResult} doesn't support).
 */
public class ConsentRequiredException extends RuntimeException {

	private final String consentUrl;

	public ConsentRequiredException(String message, String consentUrl) {
		super(message);
		this.consentUrl = consentUrl;
	}

	public String consentUrl() {
		return consentUrl;
	}
}
