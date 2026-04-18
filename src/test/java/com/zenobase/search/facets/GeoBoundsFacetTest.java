package com.zenobase.search.facets;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.search.Search;
import com.zenobase.testing.NodeAssert;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GeoBoundsFacetTest extends FacetTestSupport {

	private static final Location DENVER = new Location("39.75", "-104.87");
	private static final Location LAS_VEGAS = new Location("36.08", "-115.17");
	private static final Location SAN_DIEGO = new Location("32.82", "-117.13");

	private Event e1, e2, e3, e4;

	@BeforeEach
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
		addFacet("id:%s,type:%s", FACET_ID, GeoBoundsFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).isObject();

		node.path("lat_min").isEqualTo(32.82);
		node.path("lat_max").isEqualTo(39.75001);
		node.path("lon_min").isEqualTo(-117.13);
		node.path("lon_max").isEqualTo(-104.87);
	}

	@Test
	public void testFiltered() {
		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,filter:%s", FACET_ID, GeoBoundsFacet.TYPE, "count:[1500..*)");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).isObject();

		node.path("lat_min").isEqualTo(36.08);
		node.path("lat_max").isEqualTo(39.75001);
		node.path("lon_min").isEqualTo(-115.17);
		node.path("lon_max").isEqualTo(-104.87001);
	}

	@Test
	public void testEmpty() {
		addFacet("id:%s,type:%s", FACET_ID, GeoBoundsFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		NodeAssert node = assertThat(result).path(FACET_ID).isObject();

		node.path("lat_min").isMissingNode();
		node.path("lat_max").isMissingNode();
		node.path("lon_min").isMissingNode();
		node.path("lon_max").isMissingNode();
	}
}
