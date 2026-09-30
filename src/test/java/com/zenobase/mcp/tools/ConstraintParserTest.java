package com.zenobase.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpParametersTestAccess;
import io.helidon.jsonrpc.core.JsonRpcParams;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pinned behavior for {@link ConstraintParser} after the Helidon-MCP rewrite. Verifies op mapping (eq/ne/gt/gte/lt/lte/
 * in/contains), scalar coercion (string/number/bool), array handling for {@code in}, and the error path for malformed
 * input (which surfaces as {@link McpException} with {@code INVALID_PARAMS}).
 *
 * <p>{@code McpParameters} has no public constructor; we build one via reflection on the package-private factory by
 * parsing a JSON object and routing it through {@link JsonRpcParams}. This mirrors how Helidon's MCP dispatcher
 * constructs the {@code arguments()} param tree at runtime.
 */
public class ConstraintParserTest {

	@Test
	public void testEmptyWhenAbsent() {
		assertThat(ConstraintParser.parse(constraints("[]"))).isEmpty();
	}

	@Test
	public void testEq() {
		assertThat(
			ConstraintParser.parse(constraints("[{\"field\":\"tag\",\"op\":\"eq\",\"value\":\"run\"}]"))
		).containsExactly("tag:run");
	}

	@Test
	public void testNe() {
		assertThat(
			ConstraintParser.parse(constraints("[{\"field\":\"tag\",\"op\":\"ne\",\"value\":\"run\"}]"))
		).containsExactly("-tag:run");
	}

	@Test
	public void testGteAndLt() {
		List<String> result = ConstraintParser.parse(
			constraints(
				"[{\"field\":\"timestamp\",\"op\":\"gte\",\"value\":\"2026-01-01\"}," +
					"{\"field\":\"timestamp\",\"op\":\"lt\",\"value\":\"2026-02-01\"}]"
			)
		);
		assertThat(result).containsExactly("timestamp:[2026-01-01..*]", "timestamp:[*..2026-02-01)");
	}

	@Test
	public void testGtAndLte() {
		List<String> result = ConstraintParser.parse(
			constraints(
				"[{\"field\":\"weight\",\"op\":\"gt\",\"value\":50}," +
					"{\"field\":\"weight\",\"op\":\"lte\",\"value\":100}]"
			)
		);
		assertThat(result).containsExactly("weight:(50..*]", "weight:[*..100]");
	}

	@Test
	public void testNumericValueScalarCoercion() {
		assertThat(
			ConstraintParser.parse(constraints("[{\"field\":\"n\",\"op\":\"eq\",\"value\":42}]"))
		).containsExactly("n:42");
		assertThat(
			ConstraintParser.parse(constraints("[{\"field\":\"n\",\"op\":\"eq\",\"value\":3.14}]"))
		).containsExactly("n:3.14");
	}

	@Test
	public void testBooleanValueScalarCoercion() {
		assertThat(
			ConstraintParser.parse(constraints("[{\"field\":\"flag\",\"op\":\"eq\",\"value\":true}]"))
		).containsExactly("flag:true");
	}

	@Test
	public void testInWithStringArray() {
		assertThat(
			ConstraintParser.parse(
				constraints("[{\"field\":\"tag\",\"op\":\"in\",\"value\":[\"run\",\"walk\",\"ride\"]}]")
			)
		).containsExactly("tag:run OR walk OR ride");
	}

	@Test
	public void testInWithEmptyArrayRejected() {
		assertThatThrownBy(() ->
			ConstraintParser.parse(constraints("[{\"field\":\"tag\",\"op\":\"in\",\"value\":[]}]"))
		)
			.isInstanceOf(McpException.class)
			.hasMessageContaining("'in' op requires a non-empty array");
	}

	@Test
	public void testContains() {
		assertThat(
			ConstraintParser.parse(constraints("[{\"field\":\"note\",\"op\":\"contains\",\"value\":\"foo\"}]"))
		).containsExactly("note:*foo*");
	}

	@Test
	public void testUnknownOpRejected() {
		assertThatThrownBy(() ->
			ConstraintParser.parse(constraints("[{\"field\":\"x\",\"op\":\"regex\",\"value\":\".*\"}]"))
		)
			.isInstanceOf(McpException.class)
			.hasMessageContaining("Unknown op: regex");
	}

	@Test
	public void testMissingFieldRejected() {
		assertThatThrownBy(() -> ConstraintParser.parse(constraints("[{\"op\":\"eq\",\"value\":\"x\"}]")))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("'field'");
	}

	@Test
	public void testMissingValueRejected() {
		assertThatThrownBy(() -> ConstraintParser.parse(constraints("[{\"field\":\"x\",\"op\":\"eq\"}]")))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("'value'");
	}

	@Test
	public void testNullParameterReturnsEmpty() {
		assertThat(ConstraintParser.parse(null)).isEmpty();
	}

	/**
	 * Wraps a JSON array literal in the same {@link McpParameters} shape Helidon's dispatcher would construct from a
	 * client request. {@code constraints} is the "value" of the {@code constraints} key under {@code arguments}.
	 */
	private static McpParameters constraints(String json) {
		try (var reader = Json.createReader(new StringReader("{\"constraints\":" + json + "}"))) {
			JsonObject envelope = reader.readObject();
			JsonRpcParams params = JsonRpcParams.create(envelope);
			McpParameters root = new McpParametersTestAccess(params, envelope).build();
			return root.get("constraints");
		}
	}
}
