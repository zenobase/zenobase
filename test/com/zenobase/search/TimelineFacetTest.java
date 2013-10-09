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

public class TimelineFacetTest extends FacetTestSupport {

	private Event first, last;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		first = newEvent("2012-03-31T20:15:30Z", "5 km", 2500);
		last = newEvent("2012-05-15T08:30:00Z", "10 km", 5000);
	}

	private static Event newEvent(String timestamp, String length, int count) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		event.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf(length));
		event.setValue(Event.COUNT, count);
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s", FACET_ID, TimelineFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(3);
		node.path(0).path("label").isEqualTo("2012-03TZ");
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("label").isEqualTo("2012-04TZ");
		node.path(1).path("count").isEqualTo(0);
		node.path(2).path("label").isEqualTo("2012-05TZ");
		node.path(2).path("count").isEqualTo(1);
	}

	@Test
	public void testDefaultWithFilter() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,filter:%s", FACET_ID, TimelineFacet.TYPE, "count:(*..3000)");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(1);
		node.path(0).path("label").isEqualTo("2012-03TZ");
		node.path(0).path("count").isEqualTo(1);
	}

	@Test
	public void testDefaultWithMeasureField() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,field:%s,unit:%s", FACET_ID, TimelineFacet.TYPE, "distance", "m");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(3);
		node.path(0).path("label").isEqualTo("2012-03TZ");
		node.path(0).path("count").isEqualTo(1);
		node.path(0).path("min").isEqualTo(5000.0, "m");
		node.path(0).path("max").isEqualTo(5000.0, "m");
		node.path(0).path("avg").isEqualTo(5000.0, "m");
		node.path(1).path("label").isEqualTo("2012-04TZ");
		node.path(1).path("count").isEqualTo(0);
		node.path(1).path("min").isMissingNode();
		node.path(1).path("max").isMissingNode();
		node.path(1).path("avg").isMissingNode();
		node.path(2).path("label").isEqualTo("2012-05TZ");
		node.path(2).path("count").isEqualTo(1);
		node.path(2).path("min").isEqualTo(10000.0, "m");
		node.path(2).path("max").isEqualTo(10000.0, "m");
		node.path(2).path("avg").isEqualTo(10000.0, "m");
	}

	@Test
	public void testDefaultWithNumericField() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,field:%s", FACET_ID, TimelineFacet.TYPE, "count");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(3);
		node.path(0).path("label").isEqualTo("2012-03TZ");
		node.path(0).path("count").isEqualTo(1);
		node.path(0).path("min").isEqualTo(2500.0);
		node.path(0).path("max").isEqualTo(2500.0);
		node.path(0).path("avg").isEqualTo(2500.0);
		node.path(0).path("sum").isEqualTo(2500.0);
		node.path(1).path("label").isEqualTo("2012-04TZ");
		node.path(1).path("count").isEqualTo(0);
		node.path(1).path("min").isMissingNode();
		node.path(1).path("max").isMissingNode();
		node.path(1).path("avg").isMissingNode();
		node.path(2).path("label").isEqualTo("2012-05TZ");
		node.path(2).path("count").isEqualTo(1);
		node.path(2).path("min").isEqualTo(5000.0);
		node.path(2).path("max").isEqualTo(5000.0);
		node.path(2).path("avg").isEqualTo(5000.0);
		node.path(2).path("sum").isEqualTo(5000.0);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s", FACET_ID, TimelineFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}

	@Test
	public void testYear() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,interval:%s", FACET_ID, TimelineFacet.TYPE, "year");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(1);
		node.path(0).path("label").isEqualTo("2012TZ");
		node.path(0).path("count").isEqualTo(2);
	}

	@Test
	public void testYearWithMeasureField() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,interval:%s,field:%s,unit:%s", FACET_ID, TimelineFacet.TYPE, "year", "distance", "m");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(1);
		node.path(0).path("label").isEqualTo("2012TZ");
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("min").isEqualTo(5000.0, "m");
		node.path(0).path("max").isEqualTo(10000.0, "m");
		node.path(0).path("avg").isEqualTo(7500.0, "m");
	}

	@Test
	public void testMonth() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,interval:%s,range:%s", FACET_ID, TimelineFacet.TYPE, "month", "2012TZ");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(12);
		node.path(0).path("label").isEqualTo("2012-01TZ");
		node.path(0).path("count").isEqualTo(0);
		node.path(2).path("label").isEqualTo("2012-03TZ");
		node.path(2).path("count").isEqualTo(1);
		node.path(4).path("label").isEqualTo("2012-05TZ");
		node.path(4).path("count").isEqualTo(1);
		node.path(11).path("label").isEqualTo("2012-12TZ");
		node.path(11).path("count").isEqualTo(0);
	}

	@Test
	public void testDay() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,interval:%s,range:%s", FACET_ID, TimelineFacet.TYPE, "day", "2012-03TZ");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(31);
		node.path(0).path("label").isEqualTo("2012-03-01TZ");
		node.path(0).path("count").isEqualTo(0);
		node.path(30).path("label").isEqualTo("2012-03-31TZ");
		node.path(30).path("count").isEqualTo(1);
	}

	@Test
	public void testHour() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,interval:%s,range:%s", FACET_ID, TimelineFacet.TYPE, "hour", "2012-03-31TZ");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(24);
		node.path(0).path("label").isEqualTo("2012-03-31T00Z");
		node.path(0).path("count").isEqualTo(0);
		node.path(20).path("label").isEqualTo("2012-03-31T20Z");
		node.path(20).path("count").isEqualTo(1);
		node.path(23).path("label").isEqualTo("2012-03-31T23Z");
		node.path(23).path("count").isEqualTo(0);
	}

	@Test
	public void testHourWithTimezone() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,interval:%s,range:%s,timezone:%s", FACET_ID, TimelineFacet.TYPE, "hour", "2012-03-31T-08:00", "-08:00");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(24);
		node.path(0).path("label").isEqualTo("2012-03-31T00-08:00");
		node.path(0).path("count").isEqualTo(0);
		node.path(12).path("label").isEqualTo("2012-03-31T12-08:00");
		node.path(12).path("count").isEqualTo(1);
		node.path(23).path("label").isEqualTo("2012-03-31T23-08:00");
		node.path(23).path("count").isEqualTo(0);
	}

	@Test
	public void testMinute() {

		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,interval:%s,range:%s", FACET_ID, TimelineFacet.TYPE, "minute", "2012-03-31T20Z");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(60);
		node.path(0).path("label").isEqualTo("2012-03-31T20:00Z");
		node.path(0).path("count").isEqualTo(0);
		node.path(15).path("label").isEqualTo("2012-03-31T20:15Z");
		node.path(15).path("count").isEqualTo(1);
		node.path(59).path("label").isEqualTo("2012-03-31T20:59Z");
		node.path(59).path("count").isEqualTo(0);
	}
}
