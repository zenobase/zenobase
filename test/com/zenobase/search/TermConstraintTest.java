package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;

public class TermConstraintTest extends SearchTestSupport {

	@Before
	@Override
	public void setUp() {
		super.setUp();
		addEvent("lunch");
		addEvent("lunch");
		addEvent("dinner");
	}

	private void addEvent(String tag) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		addEvent(event);
	}

	@Test
	public void test() {
		addFilter("%s:%s", Event.TAG, "lunch");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}
}
