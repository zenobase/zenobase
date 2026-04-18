package com.zenobase.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.zenobase.models.Event;

public class JsonSchemaTest {

	@Test
	public void testEmpty() {
		JsonSchema schema = JsonSchema.forFields(List.of(), Event.READ_ONLY_FIELDS);
		assertThat(schema.type()).isEqualTo("object");
		assertThat(schema.properties()).isEmpty();
	}

	@Test
	public void testReadOnly() {
		JsonSchema schema = JsonSchema.forFields(List.of(Event.ID, Event.AUTHOR), Event.READ_ONLY_FIELDS);
		assertThat(schema.properties().get("@id").type()).isEqualTo("string");
		assertThat(schema.properties().get("@id").readOnly()).isTrue();
		assertThat(schema.properties().get("author").readOnly()).isTrue();
	}

	@Test
	public void testOneOf() {
		JsonSchema schema = JsonSchema.forFields(List.of(Event.TAG), Event.READ_ONLY_FIELDS);
		JsonSchema tag = schema.properties().get("tag");
		assertThat(tag.type()).isNull();
		assertThat(tag.readOnly()).isNull();
		assertThat(tag.oneOf()).containsExactly(JsonSchema.string(), JsonSchema.array(JsonSchema.string()));
	}

	@Test
	public void testToString() {
		String json = JsonSchema.forFields(List.of(Event.ID, Event.TAG), Event.READ_ONLY_FIELDS).toJson().toString();
		assertThat(json).isEqualTo(
			"""
			{"type":"object","properties":{\
			"@id":{"type":"string","readOnly":true},\
			"tag":{"oneOf":[\
			{"type":"string"},\
			{"type":"array","items":{"type":"string"}}]}}}"""
		);
	}

	@Test
	public void testScalarFields() {
		assertThat(Event.TAG.toJsonSchema()).isEqualTo(JsonSchema.string());
		assertThat(Event.NOTE.toJsonSchema()).isEqualTo(JsonSchema.string());
		assertThat(Event.COUNT.toJsonSchema()).isEqualTo(JsonSchema.integer());
		assertThat(Event.CURRENCY.toJsonSchema()).isEqualTo(JsonSchema.number());
		assertThat(Event.RATING.toJsonSchema()).isEqualTo(JsonSchema.integer());
		assertThat(Event.PERCENTAGE.toJsonSchema()).isEqualTo(JsonSchema.number());
		assertThat(Event.DURATION.toJsonSchema()).isEqualTo(JsonSchema.integer());
		assertThat(Event.TIMESTAMP.toJsonSchema()).isEqualTo(JsonSchema.string("date-time"));
	}

	@Test
	public void testMeasureField() {
		JsonSchema schema = Event.DISTANCE.toJsonSchema();
		assertThat(schema.type()).isEqualTo("object");
		assertThat(schema.properties()).containsExactly(
			Map.entry("@value", JsonSchema.number()),
			Map.entry("unit", JsonSchema.string())
		);
	}

	@Test
	public void testResourceField() {
		JsonSchema schema = Event.SOURCE.toJsonSchema();
		assertThat(schema.type()).isEqualTo("object");
		assertThat(schema.properties()).containsExactly(
			Map.entry("title", JsonSchema.string()),
			Map.entry("url", JsonSchema.string("uri"))
		);
	}

	@Test
	public void testLocationField() {
		JsonSchema schema = Event.LOCATION.toJsonSchema();
		assertThat(schema.type()).isEqualTo("object");
		assertThat(schema.properties()).containsExactly(
			Map.entry("lat", JsonSchema.number()),
			Map.entry("lon", JsonSchema.number())
		);
	}
}
