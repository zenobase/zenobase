package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.quantity.Length;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;

public class MeasureRangeConstraintTest extends ConstraintTestSupport {

	@Before
	public void addEvents() {
		addEvent("0 km");
		addEvent("4 km");
		addEvent("4000 m");
		addEvent("25 km");
	}

	private void addEvent(String distance) {
		Event event = new Event();
		event.setValue(Event.DISTANCE, Measures.<Length>valueOf(distance));
		addEvent(event);
	}

	@Test
	public void testRange() {
		addFilter("%s:%s", Event.DISTANCE, "[0 km..25 km]");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(4);
	}

	@Test
	public void testEmptyRange() {
		addFilter("%s:%s", Event.DISTANCE, "(4 km..25 km)");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
	}

	@Test
	public void testLowerRange() {
		addFilter("%s:%s", Event.DISTANCE, "(*..4 km]");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testUpperRange() {
		addFilter("%s:%s", Event.DISTANCE, "(4 km..*)");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidRange() {
		addFilter("%s:%s", Event.DISTANCE, "(25 km..4 km)");
		execute();
	}
}
