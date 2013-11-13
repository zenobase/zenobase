package com.zenobase.tasks.bodymedia;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Energy;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class BodyMediaSummaryResultTest extends ResultTestSupport {

	@Test
	public void test() {

		BodyMediaSummaryResult result = new BodyMediaSummaryResult(readObject("BodyMediaSummaryResultTest.json"), TESTER);
		assertThat(result.getLastSyncDate()).as("last sync date").isEqualTo(DateTime.parse("2013-01-03T13:21:40.000-12:00"));
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(4);

		Event sleep = new Event(events.get(0).getId());
		sleep.setValue(Event.TAG, BodyMediaSummaryResult.TAG_SLEEP);
		sleep.setValue(Event.TIMESTAMP, DateTime.parse("2013-01-01T12:00:00.000-12:00"));
		sleep.setValue(Event.DURATION, Duration.standardMinutes(471));
		sleep.setValue(Event.RATING, Rating.valueOf(79));
		sleep.setValue(Event.AUTHOR, TESTER);
		sleep.setValue(Event.SOURCE, BodyMediaSummaryResult.SOURCE);
		assertThat(events.get(0)).as("sleep event").isEqualTo(sleep);

		Event steps = new Event(events.get(2).getId());
		steps.setValue(Event.TAG, BodyMediaSummaryResult.TAG_STEPS);
		steps.setValue(Event.TIMESTAMP, DateTime.parse("2013-01-01T12:00:00.000-12:00"));
		steps.setValue(Event.COUNT, 12981);
		steps.setValue(Event.ENERGY, Measures.<Energy>valueOf("-2986 cal"));
		steps.setValue(Event.AUTHOR, TESTER);
		steps.setValue(Event.SOURCE, BodyMediaSummaryResult.SOURCE);
		assertThat(events.get(2)).as("steps event").isEqualTo(steps);
	}
}
