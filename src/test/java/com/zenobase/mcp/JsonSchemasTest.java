package com.zenobase.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.json.JsonSchema;
import jakarta.json.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins {@link JsonSchemas#toJsonObject} behavior against {@code JsonSchema}'s original Jackson-backed serialization.
 * Null fields must be omitted (matches the {@code @JsonInclude(NON_NULL)} on the record); nested {@code properties} /
 * {@code items} / {@code oneOf} must recurse.
 */
public class JsonSchemasTest {

	@Test
	public void testStringPrimitive() {
		JsonObject result = JsonSchemas.toJsonObject(JsonSchema.string());
		assertThat(result.getString("type")).isEqualTo("string");
		assertThat(result).hasSize(1);
	}

	@Test
	public void testStringWithFormat() {
		JsonObject result = JsonSchemas.toJsonObject(JsonSchema.string("date-time"));
		assertThat(result.getString("type")).isEqualTo("string");
		assertThat(result.getString("format")).isEqualTo("date-time");
		assertThat(result).hasSize(2);
	}

	@Test
	public void testInteger() {
		assertThat(JsonSchemas.toJsonObject(JsonSchema.integer()).getString("type")).isEqualTo("integer");
	}

	@Test
	public void testNumber() {
		assertThat(JsonSchemas.toJsonObject(JsonSchema.number()).getString("type")).isEqualTo("number");
	}

	@Test
	public void testReadOnlyPreserved() {
		JsonObject result = JsonSchemas.toJsonObject(JsonSchema.string().asReadOnly());
		assertThat(result.getBoolean("readOnly")).isTrue();
	}

	@Test
	public void testObjectWithProperties() {
		Map<String, JsonSchema> props = new LinkedHashMap<>();
		props.put("name", JsonSchema.string());
		props.put("age", JsonSchema.integer());
		JsonObject result = JsonSchemas.toJsonObject(JsonSchema.object(props));
		assertThat(result.getString("type")).isEqualTo("object");
		JsonObject properties = result.getJsonObject("properties");
		assertThat(properties.getJsonObject("name").getString("type")).isEqualTo("string");
		assertThat(properties.getJsonObject("age").getString("type")).isEqualTo("integer");
	}

	@Test
	public void testArrayRecursion() {
		JsonObject result = JsonSchemas.toJsonObject(JsonSchema.array(JsonSchema.string("date-time")));
		assertThat(result.getString("type")).isEqualTo("array");
		assertThat(result.getJsonObject("items").getString("type")).isEqualTo("string");
		assertThat(result.getJsonObject("items").getString("format")).isEqualTo("date-time");
	}

	@Test
	public void testOneOf() {
		JsonObject result = JsonSchemas.toJsonObject(JsonSchema.oneOf(JsonSchema.string(), JsonSchema.integer()));
		// `type` is null on a oneOf schema → must not appear
		assertThat(result.containsKey("type")).isFalse();
		assertThat(result.getJsonArray("oneOf")).hasSize(2);
		assertThat(result.getJsonArray("oneOf").getJsonObject(0).getString("type")).isEqualTo("string");
		assertThat(result.getJsonArray("oneOf").getJsonObject(1).getString("type")).isEqualTo("integer");
	}

	@Test
	public void testNullFieldsOmitted() {
		// Bare string has no format, properties, items, oneOf, or readOnly — none of those keys should appear.
		JsonObject result = JsonSchemas.toJsonObject(JsonSchema.string());
		assertThat(result.keySet()).containsExactly("type");
	}

	@Test
	public void testDeepNesting() {
		// Mirrors forFields' default shape: oneOf(scalar, array(scalar))
		JsonObject result = JsonSchemas.toJsonObject(
			JsonSchema.oneOf(JsonSchema.string("date-time"), JsonSchema.array(JsonSchema.string("date-time")))
		);
		assertThat(result.getJsonArray("oneOf").getJsonObject(0).getString("format")).isEqualTo("date-time");
		assertThat(result.getJsonArray("oneOf").getJsonObject(1).getJsonObject("items").getString("format")).isEqualTo(
			"date-time"
		);
	}
}
