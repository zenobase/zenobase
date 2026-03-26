package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenobase.models.Event;

public class LocalDateTimeConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@BeforeEach
	public void addEvents() {
		addEvent("2012-01-05T12:00:00Z");
		addEvent("2012-02-05T12:15:00Z");
		addEvent("2012-01-06T05:00:00-08:00");
	}

	private void addEvent(String timestamp) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		addEvent(event);
	}

	private void addEvent(String begin, String end) {
		Event event = new Event();
		event.addValue(Event.TIMESTAMP, DateTime.parse(begin));
		event.addValue(Event.TIMESTAMP, DateTime.parse(end));
		addEvent(event);
	}

	@Test
	public void testFindJanuary() {
		addConstraint("%s.month_of_year:%s", Event.TIMESTAMP, "1");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testFindThe5th() {
		addConstraint("%s.day_of_month:%s", Event.TIMESTAMP, "5");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testFindSunday() {
		addConstraint("%s.day_of_week:%s", Event.TIMESTAMP, "7");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testFindEventsEndingOnSunday() {
		addEvent("2012-02-05T12:00:00Z", "2012-02-06T12:00:00Z");
		addConstraint("%s$max.day_of_week:%s", Event.TIMESTAMP, "7");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testFindNoon() {
		addConstraint("%s.hour_of_day:%s", Event.TIMESTAMP, "12");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}
}
