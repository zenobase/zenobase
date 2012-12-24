package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.quantity.Length;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;

public class MeasureConstraintTest extends SearchTestSupport {

	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("10 km");
		e2 = newEvent("10000 m");
		e3 = newEvent("20000 m");
	}

	private static Event newEvent(String distance) {
		Event event = new Event();
		event.setValue(Event.DISTANCE, Measures.<Length>valueOf(distance));
		return event;
	}

	@Test
	public void test() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);

		addFilter(String.format("%s:%s", Event.DISTANCE, "10 km"));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}
}
