package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.testing.NodeAssert;

public class HeatmapFacetTest extends FacetTestSupport {

	private static final Location DENVER = new Location("39.75", "-104.87");
	private static final Location LAS_VEGAS = new Location("36.08", "-115.17");
	private static final Location SAN_DIEGO = new Location("32.82", "-117.13");

	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent(DENVER, 1000);
		e2 = newEvent(new Location("39.75001", "-104.87001"), 2000); // also Denver
		e3 = newEvent(LAS_VEGAS, 1500);
		e4 = newEvent(SAN_DIEGO, null);
	}

	private static Event newEvent(Location location, Integer count) {
		Event event = new Event();
		event.setValue(Event.LOCATION, location);
		if (count != null) {
			event.setValue(Event.COUNT, count);
			event.setValue(Event.DISTANCE, Measures.valueOf(new BigDecimal(count), Units.M));
		}
		return event;
	}

	@Test
	public void test() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s", FACET_ID, HeatmapFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(3);

		node.path(0).path("lat").isEqualTo(39.75);
		node.path(0).path("lon").isEqualTo(-104.87);
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("sum").isMissingNode();

		node.path(1).path("lat").isEqualTo(36.08);
		node.path(1).path("lon").isEqualTo(-115.17);
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("sum").isMissingNode();

		node.path(2).path("lat").isEqualTo(32.82);
		node.path(2).path("lon").isEqualTo(-117.13);
		node.path(2).path("count").isEqualTo(1);
		node.path(2).path("sum").isMissingNode();
	}

	@Test
	public void testAggregateCounts() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,value_field:%s", FACET_ID, HeatmapFacet.TYPE, Event.COUNT.getName());

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);

		node.path(0).path("lat").isEqualTo(39.75);
		node.path(0).path("lon").isEqualTo(-104.87);
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("sum").isEqualTo(3000.0);

		node.path(1).path("lat").isEqualTo(36.08);
		node.path(1).path("lon").isEqualTo(-115.17);
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("sum").isEqualTo(1500.0);
	}

	@Test
	public void testFilteredAggregateCounts() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,value_field:%s,filter:%s", FACET_ID, HeatmapFacet.TYPE, Event.COUNT.getName(), "count:[1500..*)");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);

		node.path(0).path("lat").isEqualTo(39.75);
		node.path(0).path("lon").isEqualTo(-104.87);
		node.path(0).path("count").isEqualTo(1);
		node.path(0).path("sum").isEqualTo(2000.0);

		node.path(1).path("lat").isEqualTo(36.08);
		node.path(1).path("lon").isEqualTo(-115.17);
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("sum").isEqualTo(1500.0);
	}

	@Test
	public void testAggregateDistances() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,value_field:%s,unit:%s", FACET_ID, HeatmapFacet.TYPE, Event.DISTANCE.getName(), "m");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);

		node.path(0).path("lat").isEqualTo(39.75);
		node.path(0).path("lon").isEqualTo(-104.87);
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("sum").isEqualTo(3000.0, "m");

		node.path(1).path("lat").isEqualTo(36.08);
		node.path(1).path("lon").isEqualTo(-115.17);
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("sum").isEqualTo(1500.0, "m");
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s", FACET_ID, HeatmapFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
