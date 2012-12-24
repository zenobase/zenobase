package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.quantity.Length;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;

public class MeasureConstraintTest extends SearchTestSupport {

	@Before
	@Override
	public void setUp() {
		super.setUp();
		addEvent("10 km");
		addEvent("10000 m");
		addEvent("20000 m");
	}

	private void addEvent(String distance) {
		Event event = new Event();
		event.setValue(Event.DISTANCE, Measures.<Length>valueOf(distance));
		addEvent(event);
	}

	@Test
	public void test() {
		addFilter(String.format("%s:%s", Event.DISTANCE, "10 km"));
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}
}
