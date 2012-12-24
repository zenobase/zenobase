package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Rating;

public class RangeConstraintTest extends SearchTestSupport {

	@Before
	@Override
	public void setUp() {
		super.setUp();
		addEvent(0);
		addEvent(40);
		addEvent(40);
		addEvent(100);
	}

	private void addEvent(int rating) {
		Event event = new Event();
		event.setValue(Event.RATING, Rating.valueOf(rating));
		addEvent(event);
	}

	@Test
	public void testRange() {
		addFilter("%s:%s", Event.RATING, "[0..100]");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(4);
	}

	@Test
	public void testEmptyRange() {
		addFilter("%s:%s", Event.RATING, "(0..40)");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
	}

	@Test
	public void testLowerRange() {
		addFilter("%s:%s", Event.RATING, "(*..50]");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testUpperRange() {
		addFilter("%s:%s", Event.RATING, "[50..*)");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidRange() {
		addFilter("%s:%s", Event.RATING, "1");
		execute();
	}
}
