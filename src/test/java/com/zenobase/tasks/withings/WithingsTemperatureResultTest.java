package com.zenobase.tasks.withings;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.measure.DecimalMeasure;

import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class WithingsTemperatureResultTest extends ResultTestSupport {

	@Test
	public void test() {
		WithingsTemperatureResult result = new WithingsTemperatureResult(
				readObject("WithingsTemperatureResultTest.json"),
				TESTER,
				"body",
				Units.F,
				DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.getStatus()).as("status").isEqualTo(0);
		assertThat(result.getMarker()).as("marker").isEqualTo("1353615011");
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(3);
		Event actual = events.get(1);
		Event expected = new Event(actual.getId());
		expected.setValue(Event.TAG, "body");
		expected.setValue(Event.TEMPERATURE, DecimalMeasure.valueOf("100.000 F"));
		expected.setValue(Event.TIMESTAMP, dateTime("2012-11-22T09:49:17-08:00"));
		expected.setValue(Event.AUTHOR, TESTER);
		expected.setValue(Event.SOURCE, WithingsTemperatureResult.SOURCE);
		assertThat(actual).isEqualTo(expected);
	}
}
