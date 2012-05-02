package com.zenobase.search;

import static com.zenobase.test.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;

public class TermConstraintTest extends WidgetTestSupport {

	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {

		super.setUp();

		e1 = new Event();
		e1.setValue(Event.TAG, "lunch");

		e2 = new Event();
		e2.setValue(Event.TAG, "lunch");

		e3 = new Event();
		e3.setValue(Event.TAG, "dinner");
	}

	@Test
	public void test() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFilter(String.format("%s:%s", Event.TAG, "lunch"));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}
}
