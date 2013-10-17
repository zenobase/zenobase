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

public class ScatterPlotFacetTest extends FacetTestSupport {

	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("2012-03-30T08:00:00Z", "4 km", 2000);
		e2 = newEvent("2012-03-30T15:00:00Z", "6 km", 4000);
		e3 = newEvent("2012-04-15T09:00:00Z", "10 km", 5000);
		e4 = newEvent("2012-04-16T09:00:00Z", "20 km", 10000);
	}

	private static Event newEvent(String timestamp, String distance, int steps) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		event.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf(distance));
		event.setValue(Event.COUNT, steps);
		return event;
	}

	@Test
	public void test() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,field_x:%s,unit_x:%s,statistic_x:%s,field_y:%s,statistic_y:%s,interval:%s",
			FACET_ID, ScatterPlotFacet.TYPE, Event.DISTANCE, "km", "avg", Event.COUNT, "sum", "day");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path(0).isEqualTo(5.0);
		node.path(0).path(1).isEqualTo(6000.0);
		node.path(1).path(0).isEqualTo(10.0);
		node.path(1).path(1).isEqualTo(5000.0);
	}

	@Test
	public void testPositiveLag() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field_x:%s,unit_x:%s,statistic_x:%s,field_y:%s,statistic_y:%s,interval:%s,lag:%d",
			FACET_ID, ScatterPlotFacet.TYPE, Event.DISTANCE, "km", "avg", Event.COUNT, "sum", "day", 1);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(1);
		node.path(0).path(0).isEqualTo(20.0);
		node.path(0).path(1).isEqualTo(5000.0);
	}

	@Test
	public void testNegativeLag() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,field_x:%s,unit_x:%s,statistic_x:%s,field_y:%s,statistic_y:%s,interval:%s,lag:%d",
			FACET_ID, ScatterPlotFacet.TYPE, Event.DISTANCE, "km", "avg", Event.COUNT, "sum", "day", -1);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(1);
		node.path(0).path(0).isEqualTo(10.0);
		node.path(0).path(1).isEqualTo(10000.0);
	}

	@Test
	public void testFiltered() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,field_x:%s,unit_x:%s,statistic_x:%s,field_y:%s,statistic_y:%s,interval:%s,filter_x:%s,filter_y:%s",
			FACET_ID, ScatterPlotFacet.TYPE, Event.DISTANCE, "km", "avg", Event.COUNT, "sum", "day", "distance:(*..10 km)", "count:(2000..*)");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(1);
		node.path(0).path(0).isEqualTo(5.0);
		node.path(0).path(1).isEqualTo(4000.0);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s,field_x:%s,unit_x:%s,field_y:%s",
			FACET_ID, ScatterPlotFacet.TYPE, Event.DISTANCE, "km", Event.COUNT);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
