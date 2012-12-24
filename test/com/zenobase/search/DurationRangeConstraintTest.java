package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;

public class DurationRangeConstraintTest extends SearchTestSupport {

	@Before
	@Override
	public void setUp() {
		super.setUp();
		addEvent(0);
		addEvent(4);
		addEvent(4);
		addEvent(25);
	}

	private void addEvent(int hours) {
		Event event = new Event();
		event.setValue(Event.DURATION, Duration.standardHours(hours));
		addEvent(event);
	}

	@Test
	public void testRange() {
		addFilter(String.format("%s:%s", Event.DURATION, "[0..1d 1h]"));
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(4);
	}

	@Test
	public void testEmptyRange() {
		addFilter(String.format("%s:%s", Event.DURATION, "(10h..20h)"));
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
	}

	@Test
	public void testLowerRange() {
		addFilter(String.format("%s:%s", Event.DURATION, "(*..10h)"));
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testUpperRange() {
		addFilter(String.format("%s:%s", Event.DURATION, "[10h..*)"));
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidRange() {
		addFilter(String.format("%s:%s", Event.DURATION, "(1)"));
		execute();
	}
}
