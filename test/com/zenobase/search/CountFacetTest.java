package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class CountFacetTest extends FacetTestSupport {

	private static final String TAG_LUNCH = "lunch";
	private static final String TAG_DINNER = "dinner";

	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent(TAG_LUNCH);
		e2 = newEvent(TAG_DINNER);
		e3 = newEvent(TAG_LUNCH);
	}

	private static Event newEvent(String tag) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,field:%s", FACET_ID, CountFacet.TYPE, Event.TAG);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_LUNCH);
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("label").isEqualTo(TAG_DINNER);
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testOrderByTermDescending() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,field:%s,order:%s", FACET_ID, CountFacet.TYPE, Event.TAG, "-term");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_LUNCH);
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("label").isEqualTo(TAG_DINNER);
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testOrderByCount() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,field:%s,order:%s", FACET_ID, CountFacet.TYPE, Event.TAG, "count");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_DINNER);
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("label").isEqualTo(TAG_LUNCH);
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testLimit() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,field:%s,limit:%s", FACET_ID, CountFacet.TYPE, Event.TAG, 1);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_LUNCH);
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("label").isEqualTo(CountFacet.LABEL_MORE);
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testOffset() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,field:%s,offset:%s", FACET_ID, CountFacet.TYPE, Event.TAG, 1);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(1);
		node.path(0).path("label").isEqualTo(TAG_DINNER);
		node.path(0).path("count").isEqualTo(1);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s,field:%s", FACET_ID, CountFacet.TYPE, Event.TAG);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
