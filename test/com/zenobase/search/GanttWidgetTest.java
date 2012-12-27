package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class GanttWidgetTest extends WidgetTestSupport {

	private Event e1, e2, e3, e4, e5;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("t1", "2012-03-31T20:15:30Z");
		e2 = newEvent("t1", "2012-05-15T08:30:00Z");
		e3 = newEvent("t2", "2012-05-15T08:30:00Z");
		e4 = newEvent("t2", null);
		e5 = newEvent("t3", null);
	}

	private static Event newEvent(String tag, String timestamp) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.TIMESTAMP, timestamp != null ? DateTime.parse(timestamp) : null);
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addEvent(e5);
		addWidget("id:%s,type:%s,field:%s", WIDGET_ID, GanttWidget.TYPE, Event.TAG);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(5);
		NodeAssert node = assertThat(result).path(WIDGET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo(e1.getValue(Event.TAG));
		node.path(0).path("count").isEqualTo(2);
		node.path(0).path("first").isEqualTo(e1.getValue(Event.TIMESTAMP).toString());
		node.path(0).path("last").isEqualTo(e2.getValue(Event.TIMESTAMP).toString());
		node.path(1).path("label").isEqualTo(e3.getValue(Event.TAG));
		node.path(1).path("first").isEqualTo(e3.getValue(Event.TIMESTAMP).toString());
		node.path(1).path("last").isEqualTo(e3.getValue(Event.TIMESTAMP).toString());
		node.path(1).path("count").isEqualTo(2);
	}

	@Test
	public void testEmpty() {

		addWidget("id:%s,type:%s,field:%s", WIDGET_ID, GanttWidget.TYPE, Event.TAG);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(WIDGET_ID).hasSize(0);
	}
}
