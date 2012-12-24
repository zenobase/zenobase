package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class TimeHistogramWidgetTest extends WidgetTestSupport {

	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("2012-11-30T15:00-08:00");
		e2 = newEvent("2012-12-11T15:00Z");
		e3 = newEvent("2012-12-02T15:00-08:00");
	}

	private static Event newEvent(String timestamp) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		return event;
	}

	@Test
	public void testHourOfDay() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget("id:%s,type:%s,field:%s,interval:%s,timezone:%s",
			WIDGET_ID, TimeHistogramWidget.TYPE, Event.TIMESTAMP, "hour_of_day", DateTimeZone.forOffsetHours(-8));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(24);
		node.path(7).path("label").isEqualTo("07");
		node.path(7).path("count").isEqualTo(1);
		node.path(15).path("label").isEqualTo("15");
		node.path(15).path("count").isEqualTo(2);
	}

	@Test
	public void testDayOfWeek() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget("id:%s,type:%s,field:%s,interval:%s,timezone:%s",
			WIDGET_ID, TimeHistogramWidget.TYPE, Event.TIMESTAMP, "day_of_week", DateTimeZone.forOffsetHours(-8));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(7);
		node.path(1).path("label").isEqualTo("Tue");
		node.path(1).path("count").isEqualTo(1);
		node.path(4).path("label").isEqualTo("Fri");
		node.path(4).path("count").isEqualTo(1);
		node.path(6).path("label").isEqualTo("Sun");
		node.path(6).path("count").isEqualTo(1);
	}

	@Test
	public void testMonthOfYear() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget("id:%s,type:%s,field:%s,interval:%s,timezone:%s",
			WIDGET_ID, TimeHistogramWidget.TYPE, Event.TIMESTAMP, "month_of_year", DateTimeZone.forOffsetHours(-8));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(12);
		node.path(10).path("label").isEqualTo("Nov");
		node.path(10).path("count").isEqualTo(1);
		node.path(11).path("label").isEqualTo("Dec");
		node.path(11).path("count").isEqualTo(2);
	}
}
