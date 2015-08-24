package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class HistogramFacetTest extends FacetTestSupport {

	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("walk", "10 km", "-11.0 C", 2000);
		e2 = newEvent("hike", "10100 m", "-10.4 C", 2500);
		e3 = newEvent("hike", "20 km", "11.0 C", 7500);
		e4 = newEvent("climb", null, "11.1 C", null);
	}

	private static Event newEvent(String tag, String distance, String temperature, Integer count) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.DISTANCE, distance != null ? DecimalMeasure.<Length>valueOf(distance) : null);
		event.setValue(Event.TEMPERATURE, temperature != null ? DecimalMeasure.<Temperature>valueOf(temperature) : null);
		event.setValue(Event.COUNT, count);
		return event;
	}

	@Test
	public void testOnCount() {

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
	public void testOnCountWithFilter() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field:%s,interval:%s,filter:%s", FACET_ID,
			HistogramFacet.TYPE, Event.COUNT, 1000, "tag:hike|count:(5000..*)");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(1);
		node.path(0).path("from").isEqualTo(7000.0);
		node.path(0).path("to").isEqualTo(8000.0);
		node.path(0).path("count").isEqualTo(1);
	}

	@Test
	public void testOnDistance() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field:%s,interval:%s,unit:%s", FACET_ID, HistogramFacet.TYPE, Event.DISTANCE, 5, Units.KM);

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
	public void testOnDistanceInMiles() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field:%s,interval:%s,unit:%s", FACET_ID, HistogramFacet.TYPE, Event.DISTANCE, 5, Units.MI);

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
	public void testOnTemperatureInCelsius() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field:%s,interval:%s,unit:%s", FACET_ID, HistogramFacet.TYPE, Event.TEMPERATURE, "1.0", Units.C);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("from").path("@value").isEqualTo(11.0);
		node.path(0).path("from").path("unit").isEqualTo("C");
		node.path(0).path("to").path("@value").isEqualTo(12.0);
		node.path(0).path("to").path("unit").isEqualTo("C");
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("from").path("@value").isEqualTo(-11.0);
		node.path(1).path("from").path("unit").isEqualTo("C");
		node.path(1).path("to").path("@value").isEqualTo(-10.0);
		node.path(1).path("to").path("unit").isEqualTo("C");
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testOnTemperatureInFractionalCelsius() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field:%s,interval:%s,unit:%s", FACET_ID, HistogramFacet.TYPE, Event.TEMPERATURE, "0.5", Units.C);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(3);
		node.path(0).path("from").path("@value").isEqualTo(11.0);
		node.path(0).path("from").path("unit").isEqualTo("C");
		node.path(0).path("to").path("@value").isEqualTo(11.5);
		node.path(0).path("to").path("unit").isEqualTo("C");
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("from").path("@value").isEqualTo(-10.5);
		node.path(1).path("from").path("unit").isEqualTo("C");
		node.path(1).path("to").path("@value").isEqualTo(-10.0);
		node.path(1).path("to").path("unit").isEqualTo("C");
		node.path(1).path("count").isEqualTo(1);
		node.path(2).path("from").path("@value").isEqualTo(-11.0);
		node.path(2).path("from").path("unit").isEqualTo("C");
		node.path(2).path("to").path("@value").isEqualTo(-10.5);
		node.path(2).path("to").path("unit").isEqualTo("C");
		node.path(2).path("count").isEqualTo(1);
	}

	@Test
	public void testOnTemperatureInFarenheit() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field:%s,interval:%s,unit:%s", FACET_ID, HistogramFacet.TYPE, Event.TEMPERATURE, "1.0", Units.F);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(3);
		node.path(0).path("from").path("@value").isEqualTo(51.0);
		node.path(0).path("from").path("unit").isEqualTo("F");
		node.path(0).path("to").path("@value").isEqualTo(52.0);
		node.path(0).path("to").path("unit").isEqualTo("F");
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("from").path("@value").isEqualTo(13.0);
		node.path(1).path("from").path("unit").isEqualTo("F");
		node.path(1).path("to").path("@value").isEqualTo(14.0);
		node.path(1).path("to").path("unit").isEqualTo("F");
		node.path(1).path("count").isEqualTo(1);
		node.path(2).path("from").path("@value").isEqualTo(12.0);
		node.path(2).path("from").path("unit").isEqualTo("F");
		node.path(2).path("to").path("@value").isEqualTo(13.0);
		node.path(2).path("to").path("unit").isEqualTo("F");
		node.path(2).path("count").isEqualTo(1);
	}

	@Test
	public void testOnEmpty() {

		addFacet("id:%s,type:%s,field:%s", FACET_ID, HistogramFacet.TYPE, Event.RATING);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
