package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Location;

public class DistanceConstraintTest extends SearchTestSupport {

	private static final Location LAS_VEGAS = new Location("36.08", "-115.17");
	private static final Location SAN_DIEGO = new Location("32.82", "-117.13");
	private static final Location DENVER = new Location("39.75", "-104.87");

	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {

		super.setUp();

		e1 = new Event();
		e1.setValue(Event.LOCATION, LAS_VEGAS);

		e2 = new Event();
		e2.setValue(Event.LOCATION, LAS_VEGAS);

		e3 = new Event();
		e3.setValue(Event.LOCATION, SAN_DIEGO);

		e4 = new Event();
		e4.setValue(Event.LOCATION, DENVER);
	}

	@Test
	public void testShortDistance() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);

		addFilter(String.format("%s:%s", Event.LOCATION, LAS_VEGAS)); // location:-115.17,36.08

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testMediumDistance() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);

		addFilter(String.format("%s:%s~%s", Event.LOCATION, LAS_VEGAS, "300 mi")); // location:-115.17,36.08~300 mi

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
	}

	@Test(expected = NumberFormatException.class)
	public void testBadDistance() {

		addFilter(String.format("%s:%s~%s", Event.LOCATION, LAS_VEGAS, "x")); // location:-115.17,36.08~x

		execute();
	}
}
