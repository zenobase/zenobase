package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import java.math.BigDecimal;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.testing.NodeAssert;

public class MapWidgetTest extends SearchTestSupport {

	private String id = Generator.id();
	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent(39.75, -104.87); // Denver
		e2 = newEvent(39.75, -104.87); // Denver
		e3 = newEvent(36.08, -115.17); // Las Vegas
		e4 = newEvent(32.82, -117.13); // San Diego
	}

	private static Event newEvent(double lat, double lon) {
		Event event = new Event();
		event.setValue(Event.LOCATION, new Location(BigDecimal.valueOf(lat), BigDecimal.valueOf(lon)));
		return event;
	}

	@Test
	public void testClusterSome() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addWidget(String.format("id:%s,type:%s,factor:%s", id, MapWidget.TYPE, 0.5));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("lat").isEqualTo(39.75);
		node.path(0).path("lon").isEqualTo(-104.87);
		node.path(1).path("count").isEqualTo(2);
		node.path(1).path("lat").isEqualTo(34.45);
		node.path(1).path("lon").isEqualTo(-116.15);
	}

	@Test
	public void testClusterAll() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addWidget(String.format("id:%s,type:%s,factor:%s", id, MapWidget.TYPE, 1.0));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(id).hasSize(1);
		node.path(0).path("count").isEqualTo(4);
		node.path(0).path("lat").isEqualTo(37.1);
		node.path(0).path("lon").isEqualTo(-110.51);
		node.path(0).path("lat_min").isEqualTo(32.82);
		node.path(0).path("lat_max").isEqualTo(39.75);
		node.path(0).path("lon_min").isEqualTo(-117.13);
		node.path(0).path("lon_max").isEqualTo(-104.87);
	}

	@Test
	public void testEmpty() {

		addWidget(String.format("id:%s,type:%s", id, MapWidget.TYPE));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(id).hasSize(0);
	}
}
