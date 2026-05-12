package com.zenobase.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

/**
 * Signals a JSON-RPC error from inside a tool/resource handler. The {@link McpJsonRpcHandler} catches this and
 * serializes it to the JSON-RPC error envelope.
 *
 * <p>Error codes follow the JSON-RPC 2.0 spec for the well-known range (-32700..-32600) and the MCP convention for
 * application-defined codes (-32xxx). We currently use:
 * <ul>
 *   <li>{@link #INVALID_PARAMS} {@code -32602} — malformed parameters (wrong type, missing field, etc.)</li>
 *   <li>{@link #METHOD_NOT_FOUND} {@code -32601} — unknown JSON-RPC method or tool/resource</li>
 *   <li>{@link #ACCESS_NOT_GRANTED} {@code -32002} — token is valid but the requested bucket has no active grant</li>
 *   <li>{@link #INTERNAL_ERROR} {@code -32603}</li>
 * </ul>
 */
public class McpException extends RuntimeException {

	public static final int INVALID_PARAMS = -32602;
	public static final int METHOD_NOT_FOUND = -32601;
	public static final int ACCESS_NOT_GRANTED = -32002;
	public static final int INTERNAL_ERROR = -32603;

	private final int code;
	private final @Nullable JsonNode data;

	public McpException(int code, String message) {
		this(code, message, null);
	}

	public McpException(int code, String message, @Nullable JsonNode data) {
		super(message);
		this.code = code;
		this.data = data;
	}

	public int getCode() {
		return code;
	}

	public @Nullable JsonNode getData() {
		return data;
	}
}
