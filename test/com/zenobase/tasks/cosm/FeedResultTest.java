package com.zenobase.tasks.cosm;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.measure.quantity.Temperature;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.tasks.ResultTestSupport;

public class FeedResultTest extends ResultTestSupport {

	@Test
	public void test() {

		FeedResult result = new FeedResult(TESTER, readObject("FeedResultTest.json"));
		List<Event> events = result.getEvents();
		assertThat(events).as("events").hasSize(4);

		Event e0 = new Event(events.get(0).getId());
		e0.setValue(Event.TIMESTAMP, DateTime.parse("2013-04-25T01:11:25.490Z"));
		e0.addValue(Event.TAG, "test");
		e0.addValue(Event.TAG, "bedroom");
		e0.setValue(Event.HUMIDITY, 43);
		e0.setValue(Event.AUTHOR, TESTER);
		e0.setValue(Event.SOURCE, FeedResult.SOURCE);
		assertThat(events.get(0)).as("first event (humidity)").isEqualTo(e0);

		Event e1 = new Event(events.get(1).getId());
		e1.setValue(Event.TIMESTAMP, DateTime.parse("2013-04-24T23:14:58.350Z"));
		e1.addValue(Event.TAG, "test");
		e1.addValue(Event.TAG, "office");
		e1.setValue(Event.TEMPERATURE, Measures.<Temperature>valueOf("23.60 C"));
		e1.setValue(Event.AUTHOR, TESTER);
		e1.setValue(Event.SOURCE, FeedResult.SOURCE);
		assertThat(events.get(1)).as("second event (temperature)").isEqualTo(e1);
	}
}
