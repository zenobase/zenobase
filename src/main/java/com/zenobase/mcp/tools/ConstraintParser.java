package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.mcp.McpException;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

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
	 * Parses a JSON node carrying a constraints array and returns the corresponding list of expression strings.
	 * The node may be {@code null} or missing, in which case an empty list is returned. Unknown ops raise an
	 * {@link McpException} with {@link McpException#INVALID_PARAMS} so the LLM can self-correct.
	 */
	public static List<String> parse(@Nullable JsonNode constraintsNode) {
		List<String> expressions = new ArrayList<>();
		if (constraintsNode == null || constraintsNode.isNull() || constraintsNode.isMissingNode()) {
			return expressions;
		}
		if (!constraintsNode.isArray()) {
			throw new McpException(McpException.INVALID_PARAMS, "constraints must be an array");
		}
		for (JsonNode entry : constraintsNode) {
			if (!entry.isObject()) {
				throw new McpException(McpException.INVALID_PARAMS, "each constraint must be an object");
			}
			String field = textRequired(entry, "field");
			String op = textRequired(entry, "op");
			JsonNode value = entry.get("value");
			if (value == null || value.isNull()) {
				throw new McpException(McpException.INVALID_PARAMS, "constraint missing 'value'");
			}
			expressions.add(toExpression(field, op, value));
		}
		return expressions;
	}

	private static String toExpression(String field, String op, JsonNode value) {
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
				McpException.INVALID_PARAMS,
				"Unknown op: " + op + " (expected eq, ne, gt, gte, lt, lte, in, contains)"
			);
		};
	}

	private static String scalar(JsonNode value) {
		if (value.isTextual()) {
			return value.asText();
		}
		if (value.isNumber() || value.isBoolean()) {
			return value.asText();
		}
		throw new McpException(McpException.INVALID_PARAMS, "constraint value must be a scalar (string/number/bool)");
	}

	private static String joinForIn(JsonNode value) {
		if (!value.isArray() || value.isEmpty()) {
			throw new McpException(McpException.INVALID_PARAMS, "'in' op requires a non-empty array value");
		}
		StringBuilder sb = new StringBuilder();
		for (JsonNode element : value) {
			if (sb.length() > 0) {
				sb.append(" OR ");
			}
			sb.append(scalar(element));
		}
		return sb.toString();
	}

	private static String textRequired(JsonNode entry, String key) {
		JsonNode node = entry.get(key);
		if (node == null || !node.isTextual() || node.asText().isBlank()) {
			throw new McpException(McpException.INVALID_PARAMS, "constraint missing '" + key + "'");
		}
		return node.asText();
	}
}
