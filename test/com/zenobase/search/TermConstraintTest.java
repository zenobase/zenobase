package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;

public class TermConstraintTest extends ConstraintTestSupport {

	@Before
	public void addEvents() {
		addEvent("lunch", "Free Pizza!", 0);
		addEvent("lunch", "Burrito at Blue Water Taco", 10);
		addEvent("dinner", "Pizza at Domani", 15);
	}

	private void addEvent(String tag, String note, int count) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.NOTE, note);
		event.setValue(Event.COUNT, count);
		addEvent(event);
	}

	@Test
	public void testWithTag() {
		addConstraint("%s:%s", Event.TAG, "lunch");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testWithNote() {
		addConstraint("%s:%s", Event.NOTE, "pizza");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testWithCount() {
		addConstraint("%s:%s", Event.COUNT, 10);
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(1);
	}
}
