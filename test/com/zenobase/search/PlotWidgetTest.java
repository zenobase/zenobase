package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class PlotWidgetTest extends WidgetTestSupport {

	private Event first, last;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		first = newEvent("2012-03-31T20:15:30Z", "5 km", 2500);
		last = newEvent("2012-05-15T08:30:00Z", "10 km", 5000);
	}

	private static Event newEvent(String timestamp, String distance, int count) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		event.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf(distance));
		event.setValue(Event.COUNT, count);
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s", WIDGET_ID, PlotWidget.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo("2012-03T+0000");
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("label").isEqualTo("2012-05T+0000");
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testDefaultWithMeasureField() {

		addEvent(first);
		addEvent(last);
		addWidget("id:%s,type:%s,field:%s,unit:%s", WIDGET_ID, PlotWidget.TYPE, "distance", "m");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(2);
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
		addWidget("id:%s,type:%s,field:%s", WIDGET_ID, PlotWidget.TYPE, "count");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(2);
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

		addWidget("id:%s,type:%s", WIDGET_ID, PlotWidget.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(WIDGET_ID).hasSize(0);
	}
}
