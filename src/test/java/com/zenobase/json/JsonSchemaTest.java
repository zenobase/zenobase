package com.zenobase.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.zenobase.models.Event;

public class JsonSchemaTest {

	@Test
	public void testEmpty() {
		JsonSchema schema = JsonSchema.forFields(List.of());
		assertThat(schema.type()).isEqualTo("object");
		assertThat(schema.properties()).isEmpty();
	}

	@Test
	public void testScalarFields() {
		JsonSchema schema = JsonSchema.forFields(List.of(
				Event.TAG, Event.NOTE, Event.COUNT, Event.CURRENCY, Event.RATING, Event.PERCENTAGE, Event.DURATION));

		assertThat(schema.properties().get("tag")).isEqualTo(JsonSchema.string());
		assertThat(schema.properties().get("note")).isEqualTo(JsonSchema.string());
		assertThat(schema.properties().get("count")).isEqualTo(JsonSchema.integer());
		assertThat(schema.properties().get("currency")).isEqualTo(JsonSchema.number());
		assertThat(schema.properties().get("rating")).isEqualTo(JsonSchema.integer());
		assertThat(schema.properties().get("percentage")).isEqualTo(JsonSchema.number());
		assertThat(schema.properties().get("duration")).isEqualTo(JsonSchema.integer());
	}

	@Test
	public void testDateTimeField() {
		JsonSchema schema = JsonSchema.forFields(List.of(Event.TIMESTAMP));
		assertThat(schema.properties().get("timestamp")).isEqualTo(JsonSchema.string("date-time"));
	}

	@Test
	public void testMeasureField() {
		JsonSchema schema = JsonSchema.forFields(List.of(Event.DISTANCE));
		JsonSchema distance = schema.properties().get("distance");
		assertThat(distance.type()).isEqualTo("object");
		assertThat(distance.properties())
				.containsExactly(
						java.util.Map.entry("@value", JsonSchema.number()),
						java.util.Map.entry("unit", JsonSchema.string()));
	}

	@Test
	public void testResourceField() {
		JsonSchema schema = JsonSchema.forFields(List.of(Event.SOURCE));
		JsonSchema source = schema.properties().get("source");
		assertThat(source.type()).isEqualTo("object");
		assertThat(source.properties())
				.containsExactly(
						java.util.Map.entry("title", JsonSchema.string()),
						java.util.Map.entry("url", JsonSchema.string("uri")));
	}

	@Test
	public void testLocationField() {
		JsonSchema schema = JsonSchema.forFields(List.of(Event.LOCATION));
		JsonSchema location = schema.properties().get("location");
		assertThat(location.type()).isEqualTo("object");
		assertThat(location.properties())
				.containsExactly(
						java.util.Map.entry("lat", JsonSchema.number()),
						java.util.Map.entry("lon", JsonSchema.number()));
	}

	@Test
	public void testToJsonOmitsNulls() {
		String json = JsonSchema.string().toJson().toString();
		assertThat(json).isEqualTo("{\"type\":\"string\"}");
	}

	@Test
	public void testToJsonPreservesPropertyOrder() {
		String json = JsonSchema.forFields(List.of(Event.TAG, Event.COUNT, Event.DISTANCE))
				.toJson()
				.toString();
		assertThat(json).isEqualTo("""
						{"type":"object","properties":{\
						"tag":{"type":"string"},\
						"count":{"type":"integer"},\
						"distance":{"type":"object","properties":{\
						"@value":{"type":"number"},\
						"unit":{"type":"string"}}}}}""");
	}
}
