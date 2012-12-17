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

public class PlotWidgetTest extends SearchTestSupport {

	private String id = Generator.id();
	private Event first, last;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		first = newEvent(new DateTime(2012, 3, 31, 20, 15, 30, DateTimeZone.UTC), "5 km", 2500);
		last = newEvent(new DateTime(2012, 5, 15, 8, 30, 00, DateTimeZone.UTC), "10 km", 5000);
	}

	private static Event newEvent(DateTime timestamp, String distance, int count) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, timestamp);
		event.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf(distance));
		event.setValue(Event.COUNT, count);
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(first);
		addEvent(last);
		addWidget(String.format("id:%s,type:%s", id, PlotWidget.TYPE));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("label").isEqualTo("2012-03T+0000");
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("label").isEqualTo("2012-05T+0000");
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testDefaultWithMeasureField() {

		addEvent(first);
		addEvent(last);
		addWidget(String.format("id:%s,type:%s,field:%s,unit:%s", id, PlotWidget.TYPE, "distance", "m"));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("label").isEqualTo("2012-03T+0000");
		node.path(0).path("count").isEqualTo(1);
		node.path(0).path("min").isEqualTo(5000.0, "m");
		node.path(0).path("max").isEqualTo(5000.0, "m");
		node.path(0).path("avg").isEqualTo(5000.0, "m");
		node.path(1).path("label").isEqualTo("2012-05T+0000");
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("min").isEqualTo(10000.0, "m");
		node.path(1).path("max").isEqualTo(10000.0, "m");
		node.path(1).path("avg").isEqualTo(10000.0, "m");
	}

	@Test
	public void testDefaultWithNumericField() {

		addEvent(first);
		addEvent(last);
		addWidget(String.format("id:%s,type:%s,field:%s", id, PlotWidget.TYPE, "count"));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("label").isEqualTo("2012-03T+0000");
		node.path(0).path("count").isEqualTo(1);
		node.path(0).path("min").isEqualTo(2500.0);
		node.path(0).path("max").isEqualTo(2500.0);
		node.path(0).path("avg").isEqualTo(2500.0);
		node.path(0).path("sum").isEqualTo(2500.0);
		node.path(1).path("label").isEqualTo("2012-05T+0000");
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("min").isEqualTo(5000.0);
		node.path(1).path("max").isEqualTo(5000.0);
		node.path(1).path("avg").isEqualTo(5000.0);
		node.path(1).path("sum").isEqualTo(5000.0);
	}

	@Test
	public void testEmpty() {

		addWidget(String.format("id:%s,type:%s", id, PlotWidget.TYPE));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(id).hasSize(0);
	}
}
