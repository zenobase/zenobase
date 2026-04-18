package com.zenobase.tasks.netatmo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class MeasurementsResultTest extends ResultTestSupport {

	private static Set<String> TYPES = Set.of("Temperature", "Pressure", "Noise", "Humidity", "CO2", "Wind", "Rain");

	@Test
	public void test5min() {
		DateTimeZone tz = DateTimeZone.forOffsetHours(-7);
		Device device = new Device("1", "test", DateTime.now(tz), DateTime.now(tz), new Location("1", "2"), TYPES);
		MeasurementsResult result = new MeasurementsResult(
			readObject("MeasurementsResultTest-5min.json"),
			TESTER,
			device,
			false
		);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(10);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2013-03-28T16:38:45-07:00"));
		expected.addValue(Event.TAG, device.getLabel());
		expected.setValue(Event.LOCATION, device.getLocation());
		expected.setValue(Event.TEMPERATURE, Measures.valueOf("25.7 C"));
		expected.setValue(Event.PRESSURE, Measures.valueOf("1009.8 hPa"));
		expected.setValue(Event.SOUND, Measures.valueOf("40 dB"));
		expected.setValue(Event.HUMIDITY, 48);
		expected.setValue(Event.RATING, Rating.valueOf(80));
		expected.setValue(Event.VELOCITY, Measures.valueOf("20 kmh"));
		expected.setValue(Event.HEIGHT, Measures.valueOf("11 mm"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, MeasurementsResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void test1h() {
		DateTimeZone tz = DateTimeZone.forOffsetHours(-7);
		Device device = new Device("1", "test", DateTime.now(tz), DateTime.now(tz), new Location("1", "2"), TYPES);
		MeasurementsResult result = new MeasurementsResult(
			readObject("MeasurementsResultTest-1h.json"),
			TESTER,
			device,
			true
		);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(16);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2014-10-27T00:00:00-07:00"));
		expected.setValue(Event.DURATION, Duration.standardHours(1));
		expected.addValue(Event.TAG, device.getLabel());
		expected.setValue(Event.LOCATION, device.getLocation());
		expected.setValue(Event.TEMPERATURE, Measures.valueOf("22.1 C"));
		expected.setValue(Event.PRESSURE, Measures.valueOf("1021.2 hPa"));
		expected.setValue(Event.SOUND, Measures.valueOf("38 dB"));
		expected.setValue(Event.HUMIDITY, 50);
		expected.setValue(Event.RATING, Rating.valueOf(60));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, MeasurementsResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}

	@Test
	public void test1hNoTypes() {
		DateTimeZone tz = DateTimeZone.forOffsetHours(-7);
		Device device = new Device("1", "test", DateTime.now(tz), DateTime.now(tz), new Location("1", "2"), Set.of());
		MeasurementsResult result = new MeasurementsResult(
			readObject("MeasurementsResultTest-1h.json"),
			TESTER,
			device,
			true
		);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(16);
		Event expected = new Event(events.get(0).getId());
		expected.setValue(Event.TIMESTAMP, dateTime("2014-10-27T00:00:00-07:00"));
		expected.setValue(Event.DURATION, Duration.standardHours(1));
		expected.addValue(Event.TAG, device.getLabel());
		expected.setValue(Event.LOCATION, device.getLocation());
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, MeasurementsResult.SOURCE);
		assertThat(events.get(0)).as("first event").isEqualTo(expected);
	}
}
