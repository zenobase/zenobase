package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.testing.NodeAssert;

public class MapFacetTest extends FacetTestSupport {

	private static final Location SEATTLE = new Location("47.6062", "-122.3321");
	private static final Location REDMOND = new Location("47.6740", "-122.1215");
	private static final Location KIRKLAND = new Location("47.6769", "-122.2060");

	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent(SEATTLE);
		e2 = newEvent(SEATTLE);
		e3 = newEvent(REDMOND);
		e4 = newEvent(KIRKLAND);
	}

	private static Event newEvent(Location location) {
		Event event = new Event();
		event.setValue(Event.LOCATION, location);
		return event;
	}

	@Test
	public void testClusterMin() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,factor:%s", FACET_ID, MapFacet.TYPE, 0.0);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(3);
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("lat").isEqualTo(47.6061);
		node.path(0).path("lon").isEqualTo(-122.3320);
		node.path(1).path("count").isEqualTo(1);
		node.path(1).path("lat").isEqualTo(47.6739);
		node.path(1).path("lon").isEqualTo(-122.1215);
	}

	@Test
	public void testClusterSome() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,factor:%s", FACET_ID, MapFacet.TYPE, 0.8);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("lat").isEqualTo(47.5928);
		node.path(0).path("lon").isEqualTo(-122.3438);
		node.path(1).path("count").isEqualTo(2);
		node.path(1).path("lat").isEqualTo(47.6367);
		node.path(1).path("lon").isEqualTo(-122.1680);
	}

	@Test
	public void testClusterMax() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,factor:%s", FACET_ID, MapFacet.TYPE, 1.0);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(1);
		node.path(0).path("count").isEqualTo(4);
		node.path(0).path("lat").isEqualTo(47.5488);
		node.path(0).path("lon").isEqualTo(-122.3438);
		node.path(0).path("lat_min").isEqualTo(46.4063);
		node.path(0).path("lat_max").isEqualTo(47.8125);
		node.path(0).path("lon_min").isEqualTo(-122.3438);
		node.path(0).path("lon_max").isEqualTo(-120.9375);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s", FACET_ID, MapFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
