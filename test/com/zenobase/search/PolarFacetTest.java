package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class PolarFacetTest extends FacetTestSupport {

	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("2012-11-30T20:00-08:00", "10 km", 2500);
		e2 = newEvent("2012-12-11T06:00Z", "10000 m", 2500);
		e3 = newEvent("2012-12-02T20:00-08:00", "20 km", 7500);
	}

	private static Event newEvent(String timestamp, String distance, Integer count) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		event.setValue(Event.DISTANCE, distance != null ? DecimalMeasure.<Length>valueOf(distance) : null);
		event.setValue(Event.COUNT, count);
		return event;
	}

	@Test
	public void testHourOfDay() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,key_field:%s,interval:%s",
			FACET_ID, PolarFacet.TYPE, Event.TIMESTAMP, "hour_of_day");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(24);

		node.path(0).path("value").isEqualTo(0);
		node.path(0).path("label").isEqualTo("00:00");
		node.path(0).path("count").isEqualTo(0);
		node.path(0).path("avg").isMissingNode();

		node.path(6).path("value").isEqualTo(6);
		node.path(6).path("label").isEqualTo("06:00");
		node.path(6).path("count").isEqualTo(1);
		node.path(6).path("avg").isMissingNode();

		node.path(20).path("value").isEqualTo(20);
		node.path(20).path("label").isEqualTo("20:00");
		node.path(20).path("count").isEqualTo(2);
		node.path(20).path("avg").isMissingNode();
	}

	@Test
	public void testDayOfWeek() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,key_field:%s,interval:%s",
			FACET_ID, PolarFacet.TYPE, Event.TIMESTAMP, "day_of_week");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(7);

		node.path(0).path("value").isEqualTo(1);
		node.path(0).path("label").isEqualTo("Mon");
		node.path(0).path("count").isEqualTo(0);
		node.path(0).path("avg").isMissingNode();

		node.path(1).path("value").isEqualTo(2);
		node.path(1).path("label").isEqualTo("Tue");
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("avg").isMissingNode();

		node.path(4).path("value").isEqualTo(5);
		node.path(4).path("label").isEqualTo("Fri");
		node.path(4).path("count").isEqualTo(1);
		node.path(4).path("avg").isMissingNode();

		node.path(6).path("value").isEqualTo(7);
		node.path(6).path("label").isEqualTo("Sun");
		node.path(6).path("count").isEqualTo(1);
		node.path(6).path("avg").isMissingNode();
	}

	@Test
	public void testDayOfMonth() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,key_field:%s,interval:%s",
			FACET_ID, PolarFacet.TYPE, Event.TIMESTAMP, "day_of_month");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(31);

		node.path(0).path("value").isEqualTo(1);
		node.path(0).path("label").isEqualTo("1st");
		node.path(0).path("count").isEqualTo(0);
		node.path(0).path("avg").isMissingNode();

		node.path(1).path("value").isEqualTo(2);
		node.path(1).path("label").isEqualTo("2nd");
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("avg").isMissingNode();

		node.path(10).path("value").isEqualTo(11);
		node.path(10).path("label").isEqualTo("11th");
		node.path(10).path("count").isEqualTo(1);
		node.path(10).path("avg").isMissingNode();

		node.path(29).path("value").isEqualTo(30);
		node.path(29).path("label").isEqualTo("30th");
		node.path(29).path("count").isEqualTo(1);
		node.path(29).path("avg").isMissingNode();
	}

	@Test
	public void testMonthOfYear() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,key_field:%s,interval:%s",
			FACET_ID, PolarFacet.TYPE, Event.TIMESTAMP, "month_of_year");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(12);

		node.path(0).path("value").isEqualTo(1);
		node.path(0).path("label").isEqualTo("Jan");
		node.path(0).path("count").isEqualTo(0);
		node.path(0).path("avg").isMissingNode();

		node.path(10).path("value").isEqualTo(11);
		node.path(10).path("label").isEqualTo("Nov");
		node.path(10).path("count").isEqualTo(1);
		node.path(10).path("avg").isMissingNode();

		node.path(11).path("value").isEqualTo(12);
		node.path(11).path("label").isEqualTo("Dec");
		node.path(11).path("count").isEqualTo(2);
		node.path(11).path("avg").isMissingNode();
	}

	@Test
	public void testMonthOfYearWithFilter() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,key_field:%s,interval:%s,filter:%s",
			FACET_ID, PolarFacet.TYPE, Event.TIMESTAMP, "month_of_year", "count:(*..5000)");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(12);

		node.path(0).path("value").isEqualTo(1);
		node.path(0).path("label").isEqualTo("Jan");
		node.path(0).path("count").isEqualTo(0);
		node.path(0).path("avg").isMissingNode();

		node.path(10).path("value").isEqualTo(11);
		node.path(10).path("label").isEqualTo("Nov");
		node.path(10).path("count").isEqualTo(1);
		node.path(10).path("avg").isMissingNode();

		node.path(11).path("value").isEqualTo(12);
		node.path(11).path("label").isEqualTo("Dec");
		node.path(11).path("count").isEqualTo(1);
		node.path(11).path("avg").isMissingNode();
	}

	@Test
	public void testMonthOfYearWithMeasureField() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,key_field:%s,value_field:%s,interval:%s,unit:%s",
			FACET_ID, PolarFacet.TYPE, Event.TIMESTAMP, Event.DISTANCE, "month_of_year", "m");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(12);

		node.path(0).path("value").isEqualTo(1);
		node.path(0).path("label").isEqualTo("Jan");
		node.path(0).path("count").isEqualTo(0);
		node.path(0).path("min").isMissingNode();
		node.path(0).path("max").isMissingNode();
		node.path(0).path("sum").isMissingNode();
		node.path(0).path("avg").isMissingNode();

		node.path(10).path("value").isEqualTo(11);
		node.path(10).path("label").isEqualTo("Nov");
		node.path(10).path("count").isEqualTo(1);
		node.path(10).path("min").isEqualTo(10000.0, "m");
		node.path(10).path("max").isEqualTo(10000.0, "m");
		node.path(10).path("sum").isEqualTo(10000.0, "m");
		node.path(10).path("avg").isEqualTo(10000.0, "m");

		node.path(11).path("label").isEqualTo("Dec");
		node.path(11).path("count").isEqualTo(2);
		node.path(11).path("min").isEqualTo(10000.0, "m");
		node.path(11).path("max").isEqualTo(20000.0, "m");
		node.path(11).path("sum").isEqualTo(30000.0, "m");
		node.path(11).path("avg").isEqualTo(15000.0, "m");
	}

	@Test
	public void testMonthOfYearWithNumericField() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,key_field:%s,value_field:%s,interval:%s",
			FACET_ID, PolarFacet.TYPE, Event.TIMESTAMP, Event.COUNT, "month_of_year");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(12);

		node.path(0).path("value").isEqualTo(1);
		node.path(0).path("label").isEqualTo("Jan");
		node.path(0).path("count").isEqualTo(0);
		node.path(0).path("min").isMissingNode();
		node.path(0).path("max").isMissingNode();
		node.path(0).path("sum").isMissingNode();
		node.path(0).path("avg").isMissingNode();

		node.path(10).path("value").isEqualTo(11);
		node.path(10).path("label").isEqualTo("Nov");
		node.path(10).path("count").isEqualTo(1);
		node.path(10).path("min").isEqualTo(2500.0);
		node.path(10).path("max").isEqualTo(2500.0);
		node.path(10).path("sum").isEqualTo(2500.0);
		node.path(10).path("avg").isEqualTo(2500.0);

		node.path(11).path("label").isEqualTo("Dec");
		node.path(11).path("count").isEqualTo(2);
		node.path(11).path("min").isEqualTo(2500.0);
		node.path(11).path("max").isEqualTo(7500.0);
		node.path(11).path("sum").isEqualTo(10000.0);
		node.path(11).path("avg").isEqualTo(5000.0);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s,key_field:%s,interval:%s",
			FACET_ID, PolarFacet.TYPE, Event.TIMESTAMP, "hour_of_day");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
