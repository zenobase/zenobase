package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.mcp.McpException;
import com.zenobase.mcp.McpJsonRpcHandler;
import com.zenobase.oauth.Authorization;

/**
 * One MCP tool. Tools are invoked by {@code tools/call} with structured JSON arguments and return a JSON result that
 * {@link McpJsonRpcHandler} wraps in the MCP response envelope.
 */
public interface McpTool {
	/** Tool name as exposed to the LLM. Noun-only by convention (e.g. {@code events}, {@code stats}). */
	String name();

	/** Short one-line description the LLM uses to pick the right tool. */
	String description();

	/** JSON Schema for the tool's input arguments. */
	ObjectNode inputSchema();

	/** Executes the tool. Implementations throw {@link McpException} for client-correctable errors. */
	JsonNode call(Authorization auth, JsonNode args);
}
