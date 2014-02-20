package com.zenobase.tasks.reporter;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Dimensionless;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.models.Rating;
import com.zenobase.tasks.ResultTestSupport;

public class SnapshotsResultTest extends ResultTestSupport {

	@Test
	public void test() {

		Configuration config = new Configuration();
		config.setTimezone(DateTimeZone.forID("America/New_York"));
		config.addQuestion(new Question("How are you 0-10?", "Mood", "rating"));
		config.addQuestion(new Question("Pick a color", "Color", null));
		config.addQuestion(new Question("What are you doing?", null, null));
		SnapshotsResult result = new SnapshotsResult(config, TESTER, readObject("SnapshotsResultTest.json"));
		List<Event> events = result.getEvents();

		assertThat(events).as("events").hasSize(5);

		Event e1 = new Event(events.get(0).getId());
		e1.setValue(Event.TIMESTAMP, DateTime.parse("2014-02-17T14:07:41.000-05:00"));
		e1.setValue(Event.AUTHOR, TESTER);
		e1.setValue(Event.SOURCE, SnapshotsResult.SOURCE);
		e1.setValue(Event.LOCATION, new Location("41.41948377668317", "-71.70834391640776"));
		e1.setValue(Event.SOUND, DecimalMeasure.<Dimensionless>valueOf("-44.06892 dB"));
		e1.addValue(Event.TAG, "Color");
		e1.addValue(Event.TAG, "Orange");
		assertThat(events.get(0)).as("1st event").isEqualTo(e1);

		Event e2 = new Event(events.get(1).getId());
		e2.setValue(Event.TIMESTAMP, DateTime.parse("2014-02-17T14:07:41.000-05:00"));
		e2.setValue(Event.AUTHOR, TESTER);
		e2.setValue(Event.SOURCE, SnapshotsResult.SOURCE);
		e2.setValue(Event.LOCATION, new Location("41.41948377668317", "-71.70834391640776"));
		e2.setValue(Event.SOUND, DecimalMeasure.<Dimensionless>valueOf("-44.06892 dB"));
		e2.addValue(Event.TAG, "Mood");
		e2.setValue(Event.RATING, Rating.valueOf(60));
		assertThat(events.get(1)).as("2nd event").isEqualTo(e2);

		Event e3 = new Event(events.get(2).getId());
		e3.setValue(Event.TIMESTAMP, DateTime.parse("2014-02-17T14:07:41.000-05:00"));
		e3.setValue(Event.AUTHOR, TESTER);
		e3.setValue(Event.SOURCE, SnapshotsResult.SOURCE);
		e3.setValue(Event.LOCATION, new Location("41.41948377668317", "-71.70834391640776"));
		e3.setValue(Event.SOUND, DecimalMeasure.<Dimensionless>valueOf("-44.06892 dB"));
		e3.addValue(Event.NOTE, "Working from home");
		assertThat(events.get(2)).as("2nd event").isEqualTo(e3);
	}
}
