package com.zenobase.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.json.Nodes;
import com.zenobase.mcp.McpException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ConstraintParserTest {

	@Test
	public void testEmptyOrNullReturnsEmpty() {
		assertThat(ConstraintParser.parse(null)).isEmpty();
		assertThat(ConstraintParser.parse(Nodes.newArray())).isEmpty();
	}

	@Test
	public void testEq() {
		List<String> exprs = ConstraintParser.parse(parse("[{\"field\":\"tag\",\"op\":\"eq\",\"value\":\"run\"}]"));
		assertThat(exprs).containsExactly("tag:run");
	}

	@Test
	public void testNe() {
		List<String> exprs = ConstraintParser.parse(parse("[{\"field\":\"tag\",\"op\":\"ne\",\"value\":\"run\"}]"));
		assertThat(exprs).containsExactly("-tag:run");
	}

	@Test
	public void testGteAndLt() {
		List<String> exprs = ConstraintParser.parse(
			parse(
				"[{\"field\":\"timestamp\",\"op\":\"gte\",\"value\":\"2026-01-01\"}," +
					"{\"field\":\"timestamp\",\"op\":\"lt\",\"value\":\"2026-04-01\"}]"
			)
		);
		assertThat(exprs).containsExactly("timestamp:[2026-01-01..*]", "timestamp:[*..2026-04-01)");
	}

	@Test
	public void testGtAndLte() {
		List<String> exprs = ConstraintParser.parse(
			parse(
				"[{\"field\":\"value\",\"op\":\"gt\",\"value\":10}," +
					"{\"field\":\"value\",\"op\":\"lte\",\"value\":20}]"
			)
		);
		assertThat(exprs).containsExactly("value:(10..*]", "value:[*..20]");
	}

	@Test
	public void testIn() {
		List<String> exprs = ConstraintParser.parse(
			parse("[{\"field\":\"tag\",\"op\":\"in\",\"value\":[\"run\",\"walk\"]}]")
		);
		assertThat(exprs).containsExactly("tag:run OR walk");
	}

	@Test
	public void testContains() {
		List<String> exprs = ConstraintParser.parse(
			parse("[{\"field\":\"note\",\"op\":\"contains\",\"value\":\"morning\"}]")
		);
		assertThat(exprs).containsExactly("note:*morning*");
	}

	@Test
	public void testUnknownOpProducesInvalidParams() {
		assertThatThrownBy(() ->
			ConstraintParser.parse(parse("[{\"field\":\"tag\",\"op\":\"matches\",\"value\":\"x\"}]"))
		)
			.isInstanceOf(McpException.class)
			.hasMessageContaining("Unknown op: matches")
			.satisfies(e -> assertThat(((McpException) e).getCode()).isEqualTo(McpException.INVALID_PARAMS));
	}

	@Test
	public void testMissingFieldProducesInvalidParams() {
		assertThatThrownBy(() -> ConstraintParser.parse(parse("[{\"op\":\"eq\",\"value\":\"x\"}]")))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("constraint missing 'field'");
	}

	@Test
	public void testNonArrayConstraintsProducesInvalidParams() {
		assertThatThrownBy(() -> ConstraintParser.parse(parse("{}")))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("constraints must be an array");
	}

	@Test
	public void testInWithEmptyArrayProducesInvalidParams() {
		assertThatThrownBy(() -> ConstraintParser.parse(parse("[{\"field\":\"tag\",\"op\":\"in\",\"value\":[]}]")))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("'in' op requires a non-empty array");
	}

	private static JsonNode parse(String json) {
		try {
			return Nodes.MAPPER.readTree(json);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
