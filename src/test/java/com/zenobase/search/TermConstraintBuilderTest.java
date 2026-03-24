package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;

public class TermConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@Before
	public void addEvents() {
		addEvent("lunch", "Free Pizza!", 0);
		addEvent("lunch", "Burrito at Blue Water Taco", 10);
		addEvent("dinner", "Pizza at Domani", 15);
		addEvent("breakfast", "Pancakes!", 5);
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
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testExcludeTag() {
		addConstraint("-%s:%s", Event.TAG, "lunch");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testWithNote() {
		addConstraint("%s:%s", Event.NOTE, "pizza");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testWithCount() {
		addConstraint("%s:%s", Event.COUNT, 10);
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testMultipleTag() {
		addConstraint("%s:%s", Event.TAG, "lunch OR dinner");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}
}
