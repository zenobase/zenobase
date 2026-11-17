package com.zenobase.mcp.tools;

import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.jsonrpc.core.JsonRpcError;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates the structured MCP tool constraint shape — a flat list of {@code {field, op, value}} objects, all
 * AND-combined — into the existing Zenobase query-string constraint format that {@code EventSearchBuilder} consumes
 * (e.g. {@code "timestamp:[2026-01-01..2026-04-01)"}, {@code "tag:run"}).
 *
 * <p>Supported ops (flat, no nesting): {@code eq}, {@code ne}, {@code gt}, {@code gte}, {@code lt}, {@code lte},
 * {@code in} (value is an array), {@code contains} (wildcard substring match).
 */
public final class ConstraintParser {

	private ConstraintParser() {}

	/**
	 * Parses the {@code constraints} entry of an MCP tool's arguments and returns the corresponding list of expression
	 * strings. Missing / null arguments map to an empty list. Unknown ops or malformed entries raise an
	 * {@link McpException} with {@link JsonRpcError#INVALID_PARAMS} so the LLM can self-correct.
	 */
	public static List<String> parse(McpParameters constraints) {
		List<String> expressions = new ArrayList<>();
		if (constraints == null || constraints.isEmpty()) {
			return expressions;
		}
		List<McpParameters> entries;
		try {
			entries = constraints.asList().orElse(List.of());
		} catch (IllegalStateException e) {
			throw new McpException(JsonRpcError.INVALID_PARAMS, "constraints must be an array");
		}
		for (McpParameters entry : entries) {
			String field = requireString(entry, "field");
			String op = requireString(entry, "op");
			McpParameters value = entry.get("value");
			if (value.isEmpty()) {
				throw new McpException(JsonRpcError.INVALID_PARAMS, "constraint missing 'value'");
			}
			expressions.add(toExpression(field, op, value));
		}
		return expressions;
	}

	private static String toExpression(String field, String op, McpParameters value) {
		return switch (op) {
			case "eq" -> field + ":" + scalar(value);
			case "ne" -> "-" + field + ":" + scalar(value);
			case "gte" -> field + ":[" + scalar(value) + "..*]";
			case "gt" -> field + ":(" + scalar(value) + "..*]";
			case "lte" -> field + ":[*.." + scalar(value) + "]";
			case "lt" -> field + ":[*.." + scalar(value) + ")";
			case "in" -> field + ":" + joinForIn(value);
			case "contains" -> field + ":*" + scalar(value) + "*";
			default -> throw new McpException(
				JsonRpcError.INVALID_PARAMS,
				"Unknown op: " + op + " (expected eq, ne, gt, gte, lt, lte, in, contains)"
			);
		};
	}

	private static String scalar(McpParameters value) {
		if (value.isString()) {
			return value.asString().orElseThrow();
		}
		if (value.isNumber()) {
			double d = value.asDouble().orElseThrow();
			if (d == Math.floor(d) && !Double.isInfinite(d) && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
				return Long.toString((long) d);
			}
			return Double.toString(d);
		}
		try {
			return Boolean.toString(value.asBoolean().orElseThrow());
		} catch (IllegalStateException e) {
			throw new McpException(
				JsonRpcError.INVALID_PARAMS,
				"constraint value must be a scalar (string/number/bool)"
			);
		}
	}

	private static String joinForIn(McpParameters value) {
		List<McpParameters> items;
		try {
			items = value.asList().orElse(List.of());
		} catch (IllegalStateException e) {
			throw new McpException(JsonRpcError.INVALID_PARAMS, "'in' op requires a non-empty array value");
		}
		if (items.isEmpty()) {
			throw new McpException(JsonRpcError.INVALID_PARAMS, "'in' op requires a non-empty array value");
		}
		StringBuilder sb = new StringBuilder();
		for (McpParameters element : items) {
			if (sb.length() > 0) {
				sb.append(" OR ");
			}
			sb.append(scalar(element));
		}
		return sb.toString();
	}

	private static String requireString(McpParameters params, String key) {
		String value;
		try {
			value = params
				.get(key)
				.asString()
				.orElseThrow(() -> new McpException(JsonRpcError.INVALID_PARAMS, "constraint missing '" + key + "'"));
		} catch (IllegalStateException e) {
			throw new McpException(JsonRpcError.INVALID_PARAMS, "constraint '" + key + "' must be a string");
		}
		if (value.isBlank()) {
			throw new McpException(JsonRpcError.INVALID_PARAMS, "constraint missing '" + key + "'");
		}
		return value;
	}
}
