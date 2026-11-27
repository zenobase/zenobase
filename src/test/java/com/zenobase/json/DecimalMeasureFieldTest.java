package com.zenobase.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pins the stored JSON format of measures, which events and the command journal depend on.
 */
public class DecimalMeasureFieldTest {

	@Test
	public void testToJsonWritesCanonicalSymbols() {
		ObjectNode node = Nodes.newObject();
		Event.VELOCITY.setValue(node, Measures.valueOf(new BigDecimal("12.50"), "kmh"));
		Event.FREQUENCY.setValue(node, Measures.valueOf(BigDecimal.ONE, Units.RPM));
		Event.PRESSURE.setValue(node, Measures.valueOf(BigDecimal.ONE, "mbar"));
		Event.TEMPERATURE.setValue(node, Measures.valueOf(BigDecimal.ONE, "°C"));
		Event.ENERGY.setValue(node, Measures.valueOf(BigDecimal.ONE, Units.KCAL));
		Event.PACE.setValue(node, Measures.valueOf(BigDecimal.ONE, Units.S_PER_MI));
		assertThat(node.toString()).isEqualTo(
			"{\"velocity\":{\"@value\":12.50,\"unit\":\"km/h\"}," +
				"\"frequency\":{\"@value\":1,\"unit\":\"bpm\"}," +
				"\"pressure\":{\"@value\":1,\"unit\":\"hPa\"}," +
				"\"temperature\":{\"@value\":1,\"unit\":\"C\"}," +
				"\"energy\":{\"@value\":1,\"unit\":\"kcal\"}," +
				"\"pace\":{\"@value\":1,\"unit\":\"s/mi\"}}"
		);
		assertThat(new Event(node).validate(null)).isNull();
	}

	@ParameterizedTest
	@CsvSource({
		"velocity, kmh, 0.2777778",
		"frequency, rpm, 0.01666667",
		"pressure, mbar, 100",
		"temperature, °C, 274.15",
		"weight, µg, 1E-9",
		"distance, m^2, 1",
	})
	public void testPrePersistAcceptsStoredSymbols(String fieldName, String symbol, String expected) {
		ObjectNode node = Nodes.readObject("{\"" + fieldName + "\":{\"@value\":1,\"unit\":\"" + symbol + "\"}}");
		String original = node.toString();
		DecimalMeasureField<?> field = field(fieldName);
		field.prePersist(node);
		assertThat(node.path(fieldName).path("_value").decimalValue()).isEqualByComparingTo(expected);
		field.postPersist(node);
		assertThat(node.toString()).isEqualTo(original);
	}

	@ParameterizedTest
	@CsvSource(
		delimiter = '|',
		value = {
			"velocity | {\"@value\":1,\"unit\":\"kmh\"}",
			"velocity | {\"@value\":1,\"unit\":\"km/h\"}",
			"velocity | [{\"@value\":1,\"unit\":\"mph\"},{\"@value\":2.5,\"unit\":\"m/s\"}]",
			"temperature | {\"@value\":-1,\"unit\":\"°F\"}",
			"volume | {\"@value\":1,\"unit\":\"cups\"}",
		}
	)
	public void testValidateAccepts(String fieldName, String json) {
		assertThat(validate(fieldName, json)).isNull();
	}

	@ParameterizedTest
	@CsvSource(
		delimiter = '|',
		value = {
			"velocity | {\"@value\":1,\"unit\":\"kg\"} | Unsupported velocity unit <kg>",
			"velocity | [{\"@value\":1,\"unit\":\"mph\"},{\"@value\":1,\"unit\":\"m/s²\"}] | Unsupported velocity unit <m/s²>",
			"weight | {\"@value\":1,\"unit\":\"µg\"} | Unsupported weight unit <µg>",
			"weight | {\"@value\":1,\"unit\":\"lbs\"} | Unsupported weight unit <lbs>",
			"weight | {\"@value\":\"1\",\"unit\":\"kg\"} | weight must be an object with a numeric @value and a unit",
			"weight | {\"@value\":1} | weight must be an object with a numeric @value and a unit",
			"weight | 1 | weight must be an object with a numeric @value and a unit",
		}
	)
	public void testValidateRejects(String fieldName, String json, String expected) {
		assertThat(validate(fieldName, json)).isEqualTo(expected);
	}

	private static String validate(String fieldName, String json) {
		ObjectNode node = Nodes.newObject();
		node.set(fieldName, Nodes.read(json));
		return field(fieldName).validate(node, null);
	}

	private static DecimalMeasureField<?> field(String name) {
		return Event.FIELDS.stream()
			.filter(f -> f.getName().equals(name))
			.map(f -> (DecimalMeasureField<?>) f)
			.findFirst()
			.orElseThrow();
	}
}
