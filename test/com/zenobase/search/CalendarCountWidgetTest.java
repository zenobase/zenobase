package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class CalendarCountWidgetTest extends SearchTestSupport {

	private String id = Generator.id();
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
		addWidget(String.format("id:%s,type:%s,field:%s,interval:%s,timezoneOffset:%d", id, CalendarCountWidget.TYPE, Event.TIMESTAMP, "hourOfDay", -480));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(id).hasSize(24);
		node.path(6).path("label").isEqualTo(7);
		node.path(6).path("count").isEqualTo(1);
		node.path(14).path("label").isEqualTo(15);
		node.path(14).path("count").isEqualTo(2);
	}

	@Test
	public void testDayOfWeek() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget(String.format("id:%s,type:%s,field:%s,interval:%s,timezoneOffset:%d", id, CalendarCountWidget.TYPE, Event.TIMESTAMP, "dayOfWeek", -480));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(id).hasSize(7);
		node.path(1).path("label").isEqualTo(2);
		node.path(1).path("count").isEqualTo(1);
		node.path(4).path("label").isEqualTo(5);
		node.path(4).path("count").isEqualTo(1);
		node.path(6).path("label").isEqualTo(7);
		node.path(6).path("count").isEqualTo(1);
	}

	@Test
	public void testMonthOfYear() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget(String.format("id:%s,type:%s,field:%s,interval:%s,timezoneOffset:%d", id, CalendarCountWidget.TYPE, Event.TIMESTAMP, "monthOfYear", -480));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(id).hasSize(12);
		node.path(10).path("label").isEqualTo(11);
		node.path(10).path("count").isEqualTo(1);
		node.path(11).path("label").isEqualTo(12);
		node.path(11).path("count").isEqualTo(2);
	}
}
