package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.mcp.McpException;
import org.jspecify.annotations.Nullable;

/** Tiny helpers for pulling typed values out of the args JSON node with clear error messages. */
final class ToolArgs {

	private ToolArgs() {}

	static String requireString(@Nullable JsonNode args, String key) {
		String value = optString(args, key);
		if (value == null || value.isBlank()) {
			throw new McpException(McpException.INVALID_PARAMS, "Missing required parameter: " + key);
		}
		return value;
	}

	static String optString(@Nullable JsonNode args, String key, String defaultValue) {
		String value = optString(args, key);
		return value != null ? value : defaultValue;
	}

	static @Nullable String optString(@Nullable JsonNode args, String key) {
		if (args == null) {
			return null;
		}
		JsonNode node = args.get(key);
		return node != null && node.isTextual() ? node.asText() : null;
	}

	static int optInt(@Nullable JsonNode args, String key, int defaultValue) {
		if (args == null) {
			return defaultValue;
		}
		JsonNode node = args.get(key);
		if (node == null || node.isNull()) {
			return defaultValue;
		}
		if (!node.isIntegralNumber()) {
			throw new McpException(McpException.INVALID_PARAMS, key + " must be an integer");
		}
		return node.intValue();
	}
}
