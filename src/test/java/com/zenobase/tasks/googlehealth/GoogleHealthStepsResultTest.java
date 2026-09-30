package com.zenobase.tasks.googlehealth;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.models.Resource;
import com.zenobase.tasks.ResultTestSupport;
import java.util.List;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

public class GoogleHealthStepsResultTest extends ResultTestSupport {

	private static final String TAG = "steps";
	private static final DateTimeZone TIMEZONE = DateTimeZone.forID("America/Los_Angeles");

	@Test
	public void test() {
		GoogleHealthStepsResult result = new GoogleHealthStepsResult(
			readObject("GoogleHealthStepsResultTest.json"),
			TAG,
			TESTER,
			TIMEZONE
		);
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(2);

		Event first = events.get(0);
		Event expected = new Event(first.getId());
		expected.setValue(Event.TAG, TAG);
		expected.setValue(Event.TIMESTAMP, dateTime("2026-04-18T00:00:00-07:00"));
		expected.addValue(Event.TIMESTAMP, dateTime("2026-04-19T00:00:00-07:00"));
		expected.setValue(Event.DURATION, Duration.standardDays(1));
		expected.setValue(Event.COUNT, 8123);
		expected.setValue(Event.AUTHOR, TESTER);
		expected.addValue(Event.SOURCE, GoogleHealthResultSupport.SOURCE);
		expected.addValue(Event.SOURCE, new Resource("Fitbit", "https://health.google/"));
		expected.addValue(Event.SOURCE, new Resource("Charge 6", "https://health.google/"));
		assertThat(first).as("first event").isEqualTo(expected);

		assertThat(events.get(1).getValues(Event.COUNT)).containsExactly(12057);
		assertThat(result.getNextPageToken()).isNull();
	}

	@Test
	public void testEmpty() {
		GoogleHealthStepsResult result = new GoogleHealthStepsResult(Nodes.newObject(), TAG, TESTER, TIMEZONE);
		assertThat(result.getEvents()).as("events").isEmpty();
		assertThat(result.getNextPageToken()).isNull();
	}
}
