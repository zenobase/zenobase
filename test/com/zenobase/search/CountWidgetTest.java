package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class CountWidgetTest extends WidgetTestSupport {

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
		addWidget("id:%s,type:%s,field:%s", WIDGET_ID, CountWidget.TYPE, Event.TAG);

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_LUNCH);
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("label").isEqualTo(TAG_DINNER);
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testOrderByTerm() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget("id:%s,type:%s,field:%s,order:%s", WIDGET_ID, CountWidget.TYPE, Event.TAG, "term");

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_DINNER);
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("label").isEqualTo(TAG_LUNCH);
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testOrderByCountReverse() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget("id:%s,type:%s,field:%s,reverse:%s", WIDGET_ID, CountWidget.TYPE, Event.TAG, true);

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(2);
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
		addWidget("id:%s,type:%s,field:%s,limit:%s", WIDGET_ID, CountWidget.TYPE, Event.TAG, 1);

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo(TAG_LUNCH);
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("label").isEqualTo(CountWidget.LABEL_MORE);
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testOffset() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addWidget("id:%s,type:%s,field:%s,offset:%s", WIDGET_ID, CountWidget.TYPE, Event.TAG, 1);

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(1);
		node.path(0).path("label").isEqualTo(TAG_DINNER);
		node.path(0).path("count").isEqualTo(1);
	}

	@Test
	public void testEmpty() {

		addWidget("id:%s,type:%s,field:%s", WIDGET_ID, CountWidget.TYPE, Event.TAG);

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(WIDGET_ID).hasSize(0);
	}
}
