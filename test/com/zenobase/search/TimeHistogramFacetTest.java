package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class TimeHistogramFacetTest extends FacetTestSupport {

	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("2012-11-30T20:00-08:00");
		e2 = newEvent("2012-12-11T06:00Z");
		e3 = newEvent("2012-12-02T20:00-08:00");
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
		addFacet("id:%s,type:%s,field:%s,interval:%s",
			FACET_ID, TimeHistogramFacet.TYPE, Event.TIMESTAMP, "hour_of_day");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(24);
		node.path(6).path("label").isEqualTo("06");
		node.path(6).path("count").isEqualTo(1);
		node.path(20).path("label").isEqualTo("20");
		node.path(20).path("count").isEqualTo(2);
	}

	@Test
	public void testDayOfWeek() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,field:%s,interval:%s",
			FACET_ID, TimeHistogramFacet.TYPE, Event.TIMESTAMP, "day_of_week");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(7);
		node.path(1).path("label").isEqualTo("Tue");
		node.path(1).path("count").isEqualTo(1);
		node.path(4).path("label").isEqualTo("Fri");
		node.path(4).path("count").isEqualTo(1);
		node.path(6).path("label").isEqualTo("Sun");
		node.path(6).path("count").isEqualTo(1);
	}

	@Test
	public void testDayOfMonth() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,field:%s,interval:%s",
			FACET_ID, TimeHistogramFacet.TYPE, Event.TIMESTAMP, "day_of_month");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(31);
		node.path(1).path("label").isEqualTo("02");
		node.path(1).path("count").isEqualTo(1);
		node.path(10).path("label").isEqualTo("11");
		node.path(10).path("count").isEqualTo(1);
		node.path(29).path("label").isEqualTo("30");
		node.path(29).path("count").isEqualTo(1);
	}

	@Test
	public void testMonthOfYear() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,field:%s,interval:%s",
			FACET_ID, TimeHistogramFacet.TYPE, Event.TIMESTAMP, "month_of_year");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(12);
		node.path(10).path("label").isEqualTo("Nov");
		node.path(10).path("count").isEqualTo(1);
		node.path(11).path("label").isEqualTo("Dec");
		node.path(11).path("count").isEqualTo(2);
	}
}
