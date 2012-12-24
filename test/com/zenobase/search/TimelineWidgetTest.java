package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class TimelineWidgetTest extends SearchTestSupport {

	private String id = Generator.id();
	private Event first, last;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		first = newEvent(new DateTime(2012, 3, 31, 20, 15, 30, DateTimeZone.UTC), "5 km", 2500);
		last = newEvent(new DateTime(2012, 5, 15, 8, 30, 00, DateTimeZone.UTC), "10 km", 5000);
	}

	private static Event newEvent(DateTime timestamp, String length, int count) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, timestamp);
		event.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf(length));
		event.setValue(Event.COUNT, count);
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s", id, TimelineWidget.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(3);
		node.path(0).path("label").isEqualTo("2012-03T+0000");
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("label").isEqualTo("2012-04T+0000");
		node.path(1).path("count").isEqualTo(0);
		node.path(2).path("label").isEqualTo("2012-05T+0000");
		node.path(2).path("count").isEqualTo(1);
	}

	@Test
	public void testDefaultWithMeasureField() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s,field:%s,unit:%s", id, TimelineWidget.TYPE, "distance", "m");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(3);
		node.path(0).path("label").isEqualTo("2012-03T+0000");
		node.path(0).path("count").isEqualTo(1);
		node.path(0).path("min").isEqualTo(5000.0, "m");
		node.path(0).path("max").isEqualTo(5000.0, "m");
		node.path(0).path("avg").isEqualTo(5000.0, "m");
		node.path(1).path("label").isEqualTo("2012-04T+0000");
		node.path(1).path("count").isEqualTo(0);
		node.path(1).path("min").isMissingNode();
		node.path(1).path("max").isMissingNode();
		node.path(1).path("avg").isMissingNode();
		node.path(2).path("label").isEqualTo("2012-05T+0000");
		node.path(2).path("count").isEqualTo(1);
		node.path(2).path("min").isEqualTo(10000.0, "m");
		node.path(2).path("max").isEqualTo(10000.0, "m");
		node.path(2).path("avg").isEqualTo(10000.0, "m");
	}

	@Test
	public void testDefaultWithNumericField() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s,field:%s", id, TimelineWidget.TYPE, "count");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(3);
		node.path(0).path("label").isEqualTo("2012-03T+0000");
		node.path(0).path("count").isEqualTo(1);
		node.path(0).path("min").isEqualTo(2500.0);
		node.path(0).path("max").isEqualTo(2500.0);
		node.path(0).path("avg").isEqualTo(2500.0);
		node.path(0).path("sum").isEqualTo(2500.0);
		node.path(1).path("label").isEqualTo("2012-04T+0000");
		node.path(1).path("count").isEqualTo(0);
		node.path(1).path("min").isMissingNode();
		node.path(1).path("max").isMissingNode();
		node.path(1).path("avg").isMissingNode();
		node.path(2).path("label").isEqualTo("2012-05T+0000");
		node.path(2).path("count").isEqualTo(1);
		node.path(2).path("min").isEqualTo(5000.0);
		node.path(2).path("max").isEqualTo(5000.0);
		node.path(2).path("avg").isEqualTo(5000.0);
		node.path(2).path("sum").isEqualTo(5000.0);
	}

	@Test
	public void testEmpty() {

		addWidget("id:%s,type:%s", id, TimelineWidget.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(id).hasSize(0);
	}

	@Test
	public void testYear() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s,interval:%s", id, TimelineWidget.TYPE, "year");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(1);
		node.path(0).path("label").isEqualTo("2012T+0000");
		node.path(0).path("count").isEqualTo(2);
	}

	@Test
	public void testYearWithMeasureField() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s,interval:%s,field:%s,unit:%s", id, TimelineWidget.TYPE, "year", "distance", "m");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(1);
		node.path(0).path("label").isEqualTo("2012T+0000");
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("min").isEqualTo(5000.0, "m");
		node.path(0).path("max").isEqualTo(10000.0, "m");
		node.path(0).path("avg").isEqualTo(7500.0, "m");
	}

	@Test
	public void testMonth() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s,interval:%s,range:%s", id, TimelineWidget.TYPE, "month", "2012T+0000");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(12);
		node.path(0).path("label").isEqualTo("2012-01T+0000");
		node.path(0).path("count").isEqualTo(0);
		node.path(2).path("label").isEqualTo("2012-03T+0000");
		node.path(2).path("count").isEqualTo(1);
		node.path(4).path("label").isEqualTo("2012-05T+0000");
		node.path(4).path("count").isEqualTo(1);
		node.path(11).path("label").isEqualTo("2012-12T+0000");
		node.path(11).path("count").isEqualTo(0);
	}

	@Test
	public void testDay() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s,interval:%s,range:%s", id, TimelineWidget.TYPE, "day", "2012-03T+0000");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(31);
		node.path(0).path("label").isEqualTo("2012-03-01T+0000");
		node.path(0).path("count").isEqualTo(0);
		node.path(30).path("label").isEqualTo("2012-03-31T+0000");
		node.path(30).path("count").isEqualTo(1);
	}

	@Test
	public void testHour() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s,interval:%s,range:%s", id, TimelineWidget.TYPE, "hour", "2012-03-31T+0000");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(24);
		node.path(0).path("label").isEqualTo("2012-03-31T00+0000");
		node.path(0).path("count").isEqualTo(0);
		node.path(20).path("label").isEqualTo("2012-03-31T20+0000");
		node.path(20).path("count").isEqualTo(1);
		node.path(23).path("label").isEqualTo("2012-03-31T23+0000");
		node.path(23).path("count").isEqualTo(0);
	}

	@Test
	public void testHourWithTimezone() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s,interval:%s,range:%s,timezone:%s", id, TimelineWidget.TYPE, "hour", "2012-03-31T-0800", "-08:00");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(24);
		node.path(0).path("label").isEqualTo("2012-03-31T00-0800");
		node.path(0).path("count").isEqualTo(0);
		node.path(12).path("label").isEqualTo("2012-03-31T12-0800");
		node.path(12).path("count").isEqualTo(1);
		node.path(23).path("label").isEqualTo("2012-03-31T23-0800");
		node.path(23).path("count").isEqualTo(0);
	}

	@Test
	public void testMinute() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s,interval:%s,range:%s", id, TimelineWidget.TYPE, "minute", "2012-03-31T20+0000");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(60);
		node.path(0).path("label").isEqualTo("2012-03-31T20:00+0000");
		node.path(0).path("count").isEqualTo(0);
		node.path(15).path("label").isEqualTo("2012-03-31T20:15+0000");
		node.path(15).path("count").isEqualTo(1);
		node.path(59).path("label").isEqualTo("2012-03-31T20:59+0000");
		node.path(59).path("count").isEqualTo(0);
	}
}
