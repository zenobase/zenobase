package com.zenobase.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.zenobase.json.Nodes;
import com.zenobase.mcp.resources.BucketResourceProvider;
import com.zenobase.mcp.tools.McpTool;
import com.zenobase.oauth.Authorization;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hand-rolled JSON-RPC 2.0 dispatcher for the MCP protocol. Handles the request/response methods MCP MVP needs:
 * {@code initialize}, {@code tools/list}, {@code tools/call}, {@code resources/list}, {@code resources/read}, and
 * {@code ping}. Notifications (no {@code id}) return no response.
 *
 * <p>Resource subscriptions and other streaming methods are intentionally not supported — MVP transport is plain
 * {@code application/json}, no SSE; see the design doc.
 */
public class McpJsonRpcHandler {

	private static final Logger logger = LoggerFactory.getLogger(McpJsonRpcHandler.class);

	private static final String PROTOCOL_VERSION = "2025-06-18";
	private static final String SERVER_NAME = "zenobase";
	private static final String SERVER_VERSION = "1";

	private final ImmutableMap<String, McpTool> tools;
	private final BucketResourceProvider buckets;

	@Inject
	public McpJsonRpcHandler(Set<McpTool> tools, BucketResourceProvider buckets) {
		ImmutableMap.Builder<String, McpTool> map = ImmutableMap.builder();
		for (McpTool tool : tools) {
			map.put(tool.name(), tool);
		}
		this.tools = map.build();
		this.buckets = buckets;
	}

	/**
	 * Dispatches a single JSON-RPC request. Returns the JSON-RPC response, or {@code null} for notifications.
	 */
	public @Nullable ObjectNode handle(Authorization auth, JsonNode request) {
		if (!request.isObject()) {
			return error(null, McpException.INVALID_PARAMS, "Request must be a JSON object");
		}
		JsonNode idNode = request.get("id");
		String method = textOrEmpty(request, "method");
		JsonNode params = request.get("params");
		boolean isNotification = idNode == null;
		try {
			ObjectNode result = dispatch(auth, method, params);
			if (isNotification) {
				return null;
			}
			ObjectNode response = Nodes.newObject();
			response.put("jsonrpc", "2.0");
			response.set("id", idNode);
			response.set("result", result);
			return response;
		} catch (McpException e) {
			String message = e.getMessage() != null ? e.getMessage() : "Error";
			if (isNotification) {
				logger.debug("Swallowing error on notification: {}", message);
				return null;
			}
			return error(idNode, e.getCode(), message, e.getData());
		} catch (RuntimeException e) {
			logger.warn("Unhandled error in MCP method {}", method, e);
			if (isNotification) {
				return null;
			}
			return error(idNode, McpException.INTERNAL_ERROR, "Internal error");
		}
	}

	private ObjectNode dispatch(Authorization auth, String method, @Nullable JsonNode params) {
		return switch (method) {
			case "initialize" -> initialize();
			case "ping" -> Nodes.newObject();
			case "tools/list" -> toolsList();
			case "tools/call" -> toolsCall(auth, params);
			case "resources/list" -> buckets.list(auth);
			case "resources/read" -> resourcesRead(auth, params);
			case "notifications/initialized" -> Nodes.newObject();
			default -> throw new McpException(McpException.METHOD_NOT_FOUND, "Unknown method: " + method);
		};
	}

	private ObjectNode initialize() {
		ObjectNode result = Nodes.newObject();
		result.put("protocolVersion", PROTOCOL_VERSION);
		ObjectNode capabilities = result.putObject("capabilities");
		capabilities.putObject("tools");
		capabilities.putObject("resources");
		ObjectNode serverInfo = result.putObject("serverInfo");
		serverInfo.put("name", SERVER_NAME);
		serverInfo.put("version", SERVER_VERSION);
		// Server-supplied system prompt addendum (per MCP spec). Teaches the model the consent model so it can guide
		// the user through the first-run flow (call `buckets`, then point at the consent URL if empty) without relying
		// on the user knowing to look at `_meta.consent_url` themselves.
		result.put(
			"instructions",
			"Zenobase is the user's personal data tracker. Call the `buckets` tool first to see what data the user " +
				"has granted you access to. If `buckets` returns an empty list with a `_meta.consent_url`, tell the user " +
				"they need to grant bucket access at that URL (the \"Access you've granted others\" section of Settings) " +
				"before you can read any data. When the user asks about specific data, use the appropriate tool " +
				"(events/histogram/stats/terms/timeline) with a bucket_id from the `buckets` listing."
		);
		return result;
	}

	private ObjectNode toolsList() {
		ObjectNode result = Nodes.newObject();
		ArrayNode array = result.putArray("tools");
		for (McpTool tool : tools.values()) {
			ObjectNode node = Nodes.newObject();
			node.put("name", tool.name());
			node.put("description", tool.description());
			node.set("inputSchema", tool.inputSchema());
			array.add(node);
		}
		return result;
	}

	private ObjectNode toolsCall(Authorization auth, @Nullable JsonNode params) {
		if (params == null || !params.isObject()) {
			throw new McpException(McpException.INVALID_PARAMS, "tools/call requires params");
		}
		String name = textOrEmpty(params, "name");
		McpTool tool = tools.get(name);
		if (tool == null) {
			throw new McpException(McpException.METHOD_NOT_FOUND, "Unknown tool: " + name);
		}
		JsonNode args = params.get("arguments");
		JsonNode payload = tool.call(auth, args);
		ObjectNode result = Nodes.newObject();
		ArrayNode content = result.putArray("content");
		ObjectNode block = Nodes.newObject();
		block.put("type", "text");
		block.put("text", payload.toString());
		content.add(block);
		ObjectNode structured = Nodes.newObject();
		structured.set("data", payload);
		result.set("structuredContent", structured);
		return result;
	}

	private ObjectNode resourcesRead(Authorization auth, @Nullable JsonNode params) {
		if (params == null || !params.isObject()) {
			throw new McpException(McpException.INVALID_PARAMS, "resources/read requires params");
		}
		String uri = textOrEmpty(params, "uri");
		if (uri.isBlank()) {
			throw new McpException(McpException.INVALID_PARAMS, "resources/read requires a uri");
		}
		return buckets.read(auth, uri);
	}

	private static ObjectNode error(@Nullable JsonNode id, int code, String message) {
		return error(id, code, message, null);
	}

	private static ObjectNode error(@Nullable JsonNode id, int code, String message, @Nullable JsonNode data) {
		ObjectNode response = Nodes.newObject();
		response.put("jsonrpc", "2.0");
		if (id != null) {
			response.set("id", id);
		} else {
			response.putNull("id");
		}
		ObjectNode error = response.putObject("error");
		error.put("code", code);
		error.put("message", message);
		if (data != null) {
			error.set("data", data);
		}
		return response;
	}

	private static String textOrEmpty(JsonNode node, String key) {
		JsonNode value = node.get(key);
		return value != null && value.isTextual() ? value.asText() : "";
	}

	/** Test/inspection accessor — returns the set of registered tool names in insertion order. */
	public Map<String, McpTool> getTools() {
		return new LinkedHashMap<>(tools);
	}
}
