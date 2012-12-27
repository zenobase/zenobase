package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.testing.NodeAssert;

public class MapWidgetTest extends WidgetTestSupport {

	private static final Location DENVER = new Location("39.75", "-104.87");
	private static final Location LAS_VEGAS = new Location("36.08", "-115.17");
	private static final Location SAN_DIEGO = new Location("32.82", "-117.13");

	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent(DENVER);
		e2 = newEvent(DENVER);
		e3 = newEvent(LAS_VEGAS);
		e4 = newEvent(SAN_DIEGO);
	}

	private static Event newEvent(Location location) {
		Event event = new Event();
		event.setValue(Event.LOCATION, location);
		return event;
	}

	@Test
	public void testClusterSome() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addWidget("id:%s,type:%s,factor:%s", WIDGET_ID, MapWidget.TYPE, 0.5);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(2);
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
		addWidget("id:%s,type:%s,factor:%s", WIDGET_ID, MapWidget.TYPE, 1.0);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(1);
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

		addWidget("id:%s,type:%s", WIDGET_ID, MapWidget.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(WIDGET_ID).hasSize(0);
	}
}
