package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
		e1 = newEvent(DENVER);
		e2 = newEvent(new Location("39.75001", "-104.87001")); // also Denver
		e3 = newEvent(LAS_VEGAS);
		e4 = newEvent(SAN_DIEGO);
	}

	private static Event newEvent(Location location) {
		Event event = new Event();
		event.setValue(Event.LOCATION, location);
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
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("lat").isEqualTo(39.75);
		node.path(0).path("lon").isEqualTo(-104.87);
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("lat").isEqualTo(36.08);
		node.path(1).path("lon").isEqualTo(-115.17);
		node.path(2).path("count").isEqualTo(1);
		node.path(2).path("lat").isEqualTo(32.82);
		node.path(2).path("lon").isEqualTo(-117.13);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s", FACET_ID, HeatmapFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
