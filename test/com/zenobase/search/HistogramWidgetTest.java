package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class HistogramWidgetTest extends SearchTestSupport {

	private String id = Generator.id();
	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("walk", new DecimalMeasure<Length>(new BigDecimal(10), SI.KILOMETER), 2000);
		e2 = newEvent("hike", new DecimalMeasure<Length>(new BigDecimal(10100), SI.METER), 2500);
		e3 = newEvent("hike", new DecimalMeasure<Length>(new BigDecimal(20), SI.KILOMETER), 7500);
		e4 = newEvent("climb", null, null);
	}

	private static Event newEvent(String tag, DecimalMeasure<Length> distance, Integer count) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.DISTANCE, distance);
		event.setValue(Event.COUNT, count);
		return event;
	}

	@Test
	public void testNumericField() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addWidget(String.format("id:%s,type:%s,field:%s,interval:%s", id, HistogramWidget.TYPE, Event.COUNT, 1000));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("value").isEqualTo(7000);
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("value").isEqualTo(2000);
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testMeasureField() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addWidget(String.format("id:%s,type:%s,field:%s,interval:%s,unit:%s", id, HistogramWidget.TYPE, Event.DISTANCE, 5, SI.KILOMETER));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("value").path("@value").isEqualTo(20.0);
		node.path(0).path("value").path("unit").isEqualTo("km");
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("value").path("@value").isEqualTo(10.0);
		node.path(1).path("value").path("unit").isEqualTo("km");
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testMeasureFieldNonSI() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addWidget(String.format("id:%s,type:%s,field:%s,interval:%s,unit:%s", id, HistogramWidget.TYPE, Event.DISTANCE, 5, NonSI.MILE));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("value").path("@value").isEqualTo(10.0);
		node.path(0).path("value").path("unit").isEqualTo("mi");
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("value").path("@value").isEqualTo(5.0);
		node.path(1).path("value").path("unit").isEqualTo("mi");
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testEmpty() {

		addWidget(String.format("id:%s,type:%s,field:%s", id, HistogramWidget.TYPE, Event.RATING));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(id).hasSize(0);
	}
}
