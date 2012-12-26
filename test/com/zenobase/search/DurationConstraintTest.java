package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.DurationFormat;
import com.zenobase.models.Event;

public class DurationConstraintTest extends ConstraintTestSupport {

	@Before
	public void addEvents() {
		addEvent("1d");
		addEvent("1d 1h");
		addEvent("1h");
	}

	private void addEvent(String duration) {
		Event event = new Event();
		event.setValue(Event.DURATION, DurationFormat.parse(duration));
		addEvent(event);
	}

	@Test
	public void test() {
		addConstraint("%s:%s", Event.DURATION, "1d 1h");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(1);
	}
}
