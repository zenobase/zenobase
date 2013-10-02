package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class HistogramFacetTest extends FacetTestSupport {

	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("walk", "10 km", 2000);
		e2 = newEvent("hike", "10100 m", 2500);
		e3 = newEvent("hike", "20 km", 7500);
		e4 = newEvent("climb", null, null);
	}

	private static Event newEvent(String tag, String distance, Integer count) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.DISTANCE, distance != null ? DecimalMeasure.<Length>valueOf(distance) : null);
		event.setValue(Event.COUNT, count);
		return event;
	}

	@Test
	public void testNumericField() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field:%s,interval:%s", FACET_ID, HistogramFacet.TYPE, Event.COUNT, 1000);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("from").isEqualTo(7000.0);
		node.path(0).path("to").isEqualTo(8000.0);
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("from").isEqualTo(2000.0);
		node.path(1).path("to").isEqualTo(3000.0);
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testMeasureField() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field:%s,interval:%s,unit:%s", FACET_ID, HistogramFacet.TYPE, Event.DISTANCE, 5, SI.KILOMETER);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("from").path("@value").isEqualTo(20.0);
		node.path(0).path("from").path("unit").isEqualTo("km");
		node.path(0).path("to").path("@value").isEqualTo(25.0);
		node.path(0).path("to").path("unit").isEqualTo("km");
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("from").path("@value").isEqualTo(10.0);
		node.path(1).path("from").path("unit").isEqualTo("km");
		node.path(1).path("to").path("@value").isEqualTo(15.0);
		node.path(1).path("to").path("unit").isEqualTo("km");
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testMeasureFieldNonSI() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field:%s,interval:%s,unit:%s", FACET_ID, HistogramFacet.TYPE, Event.DISTANCE, 5, NonSI.MILE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("from").path("@value").isEqualTo(10.0);
		node.path(0).path("from").path("unit").isEqualTo("mi");
		node.path(0).path("to").path("@value").isEqualTo(15.0);
		node.path(0).path("to").path("unit").isEqualTo("mi");
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("from").path("@value").isEqualTo(5.0);
		node.path(1).path("from").path("unit").isEqualTo("mi");
		node.path(1).path("to").path("@value").isEqualTo(10.0);
		node.path(1).path("to").path("unit").isEqualTo("mi");
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s,field:%s", FACET_ID, HistogramFacet.TYPE, Event.RATING);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
