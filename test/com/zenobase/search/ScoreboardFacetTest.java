package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;

import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class ScoreboardFacetTest extends FacetTestSupport {

	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("walk", "10 km", 2500);
		e2 = newEvent("hike", "10000 m", 2500);
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
	public void testMeasureField() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,key_field:%s,value_field:%s,unit:%s,order:%s",
			FACET_ID, ScoreboardFacet.TYPE, Event.TAG, Event.DISTANCE, "km", "total");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo(e2.getValue(Event.TAG));
		node.path(0).path("min").path("@value").isEqualTo(10.0);
		node.path(0).path("min").path("unit").isEqualTo("km");
		node.path(0).path("max").path("@value").isEqualTo(20.0);
		node.path(0).path("max").path("unit").isEqualTo("km");
		node.path(0).path("avg").path("@value").isEqualTo(15.0);
		node.path(0).path("avg").path("unit").isEqualTo("km");
		node.path(0).path("sum").path("@value").isEqualTo(30.0);
		node.path(0).path("sum").path("unit").isEqualTo("km");
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("label").isEqualTo(e1.getValue(Event.TAG));
		node.path(1).path("min").path("@value").isEqualTo(10.0);
		node.path(1).path("max").path("@value").isEqualTo(10.0);
		node.path(1).path("avg").path("@value").isEqualTo(10.0);
		node.path(1).path("sum").path("@value").isEqualTo(10.0);
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testNumericField() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,key_field:%s,value_field:%s",
			FACET_ID, ScoreboardFacet.TYPE, Event.TAG, Event.COUNT);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo(e2.getValue(Event.TAG));
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("min").isEqualTo(2500.0);
		node.path(0).path("max").isEqualTo(7500.0);
		node.path(0).path("avg").isEqualTo(5000.0);
		node.path(0).path("sum").isEqualTo(10000.0);
		node.path(1).path("label").isEqualTo(e1.getValue(Event.TAG));
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("min").isEqualTo(2500.0);
		node.path(1).path("max").isEqualTo(2500.0);
		node.path(1).path("avg").isEqualTo(2500.0);
		node.path(1).path("sum").isEqualTo(2500.0);
	}

	@Test
	public void testNumericFieldFiltered() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,key_field:%s,value_field:%s,filter:%s",
			FACET_ID, ScoreboardFacet.TYPE, Event.TAG, Event.COUNT, "tag:walk");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(1);
		node.path(0).path("label").isEqualTo(e1.getValue(Event.TAG));
		node.path(0).path("count").isEqualTo(1);
		node.path(0).path("min").isEqualTo(2500.0);
		node.path(0).path("max").isEqualTo(2500.0);
		node.path(0).path("avg").isEqualTo(2500.0);
		node.path(0).path("sum").isEqualTo(2500.0);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s,key_field:%s,value_field:%s,unit:%s,order:%s", FACET_ID, ScoreboardFacet.TYPE, Event.TAG, Event.DISTANCE, "km", "total");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
