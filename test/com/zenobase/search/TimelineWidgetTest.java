package com.zenobase.search;

import static com.zenobase.test.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.test.NodeAssert;

public class TimelineWidgetTest extends WidgetTestSupport {

	private String id = Generator.id();
	private Event first, last;

	@Before
	@Override
	public void setUp() {

		super.setUp();

		first = new Event();
		first.setValue(Event.TIMESTAMP, new DateTime(2012, 3, 31, 20, 15, 30, DateTimeZone.UTC));

		last = new Event();
		last.setValue(Event.TIMESTAMP, new DateTime(2012, 5, 15, 8, 30, 00, DateTimeZone.UTC));
	}

	@Test
	public void testDefault() {

		add(first);
		add(last);

		ObjectNode result = execute(String.format("id:%s,type:%s", id, "timeline"));
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
	public void testEmpty() {

		ObjectNode result = execute(String.format("id:%s,type:%s", id, "timeline"));
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(id).hasSize(0);
	}

	@Test
	public void testYear() {

		add(first);
		add(last);

		ObjectNode result = execute(String.format("id:%s,type:%s,interval:%s", id, "timeline", "year"));
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(1);
		node.path(0).path("label").isEqualTo("2012T+0000");
		node.path(0).path("count").isEqualTo(2);
	}

	@Test
	public void testMonth() {

		add(first);
		add(last);

		ObjectNode result = execute(String.format("id:%s,type:%s,interval:%s,range:%s", id, "timeline", "month", "2012T+0000"));
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

		add(first);
		add(last);

		ObjectNode result = execute(String.format("id:%s,type:%s,interval:%s,range:%s", id, "timeline", "day", "2012-03T+0000"));
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(31);
		node.path(0).path("label").isEqualTo("2012-03-01T+0000");
		node.path(0).path("count").isEqualTo(0);
		node.path(30).path("label").isEqualTo("2012-03-31T+0000");
		node.path(30).path("count").isEqualTo(1);
	}

	@Test
	public void testHour() {

		add(first);
		add(last);

		ObjectNode result = execute(String.format("id:%s,type:%s,interval:%s,range:%s", id, "timeline", "hour", "2012-03-31T+0000"));
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

		add(first);
		add(last);

		ObjectNode result = execute(String.format("id:%s,type:%s,interval:%s,range:%s,timezone:%s", id, "timeline", "hour", "2012-03-31T-0800", "-08:00"));
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

		add(first);
		add(last);

		ObjectNode result = execute(String.format("id:%s,type:%s,interval:%s,range:%s", id, "timeline", "minute", "2012-03-31T20+0000"));
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
