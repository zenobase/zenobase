package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Rating;

public class DecimalRangeConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@Before
	public void addEvents() {
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
		addConstraint("%s:%s", Event.RATING, "[0..100]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
	}

	@Test
	public void testEmptyRange() {
		addConstraint("%s:%s", Event.RATING, "(0..40)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
	}

	@Test
	public void testLowerRange() {
		addConstraint("%s:%s", Event.RATING, "(*..50]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testUpperRange() {
		addConstraint("%s:%s", Event.RATING, "[50..*)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidRange() {
		addConstraint("%s:%s", Event.RATING, "[100..0]");
		execute();
	}
}
