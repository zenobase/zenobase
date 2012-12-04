package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.testing.NodeAssert;

public class RatingsWidgetTest extends SearchTestSupport {

	private String id = Generator.id();
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
		addWidget(String.format("id:%s,type:%s,field:%s", id, RatingsWidget.TYPE, Event.RATING));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(5);
		NodeAssert node = assertThat(result).path(id).hasSize(4);
		node.path(0).path("from").isEqualTo(90.0);
		node.path(0).path("to").isMissingNode();
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("from").isEqualTo(50.0);
		node.path(1).path("to").isEqualTo(70.0);
		node.path(1).path("count").isEqualTo(1);
		node.path(2).path("from").isEqualTo(10.0);
		node.path(2).path("to").isEqualTo(30.0);
		node.path(2).path("count").isEqualTo(2);
		node.path(3).path("from").isMissingNode();
		node.path(3).path("to").isEqualTo(10.0);
		node.path(3).path("count").isEqualTo(1);
	}

	@Test
	public void testEmpty() {

		addWidget(String.format("id:%s,type:%s,field:%s", id, RatingsWidget.TYPE, Event.RATING));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(id).hasSize(0);
	}
}
