package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.testing.NodeAssert;

public class ListFacetTest extends FacetTestSupport {

	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		Identity principal = new Identity();
		e1 = newEvent(principal, "alpha", 1);
		e2 = newEvent(principal, "gamma", 2);
		e3 = newEvent(principal, "beta", 3);
	}

	private Event newEvent(Identity principal, String tag, int hoursAgo) {
		Event event = new Event();
		event.setValue(Event.AUTHOR, principal);
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC).minusHours(hoursAgo));
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(e2);
		addEvent(e1);
		addEvent(e3);
		addFacet("id:%s,type:%s", FACET_ID, ListFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(3);
		node.path(0).isEqualTo(e1.toJson());
		node.path(1).isEqualTo(e2.toJson());
		node.path(2).isEqualTo(e3.toJson());
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s", FACET_ID, ListFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}

	@Test
	public void testOrderByTag() {

		addEvent(e2);
		addEvent(e1);
		addEvent(e3);
		addFacet("id:%s,type:%s,offset:%d,limit:%d,order:%s", FACET_ID, ListFacet.TYPE, 1, 1, Event.TAG.getName());

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		assertThat(result).path(FACET_ID).hasSize(1);
		assertThat(result).path(FACET_ID).path(0).isEqualTo(e3.toJson());
	}
}
