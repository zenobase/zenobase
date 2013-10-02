package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.testing.NodeAssert;

public class RatingsFacetTest extends FacetTestSupport {

	private Event e1, e2, e3, e4, e5;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent(0);
		e2 = newEvent(10);
		e3 = newEvent(20);
		e4 = newEvent(50);
		e5 = newEvent(100);
	}

	private static Event newEvent(int rating) {
		Event event = new Event();
		event.setValue(Event.RATING, Rating.valueOf(rating));
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addEvent(e5);
		addFacet("id:%s,type:%s", FACET_ID, RatingsFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(5);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(4);
		node.path(0).path("from").isEqualTo(90);
		node.path(0).path("to").isMissingNode();
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("from").isEqualTo(50);
		node.path(1).path("to").isEqualTo(70);
		node.path(1).path("count").isEqualTo(1);
		node.path(2).path("from").isEqualTo(10);
		node.path(2).path("to").isEqualTo(30);
		node.path(2).path("count").isEqualTo(2);
		node.path(3).path("from").isMissingNode();
		node.path(3).path("to").isEqualTo(10);
		node.path(3).path("count").isEqualTo(1);
	}

	@Test
	public void testWithScaleZeroToTen() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addEvent(e5);
		addFacet("id:%s,type:%s,scale:%d", FACET_ID, RatingsFacet.TYPE, 10);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(5);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(5);
		node.path(0).path("from").isEqualTo(95);
		node.path(0).path("to").isMissingNode();
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("from").isEqualTo(45);
		node.path(1).path("to").isEqualTo(55);
		node.path(1).path("count").isEqualTo(1);
		node.path(2).path("from").isEqualTo(15);
		node.path(2).path("to").isEqualTo(25);
		node.path(2).path("count").isEqualTo(1);
		node.path(3).path("from").isEqualTo(5);
		node.path(3).path("to").isEqualTo(15);
		node.path(3).path("count").isEqualTo(1);
		node.path(4).path("from").isMissingNode();
		node.path(4).path("to").isEqualTo(5);
		node.path(4).path("count").isEqualTo(1);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s", FACET_ID, RatingsFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
