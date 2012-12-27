package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.DurationFormat;
import com.zenobase.models.Event;

public class DurationConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@Before
	public void addEvents() {
		addEvent("0s");
		addEvent("4h");
		addEvent("240min");
		addEvent("1d 1h");
	}

	private void addEvent(String duration) {
		Event event = new Event();
		event.setValue(Event.DURATION, DurationFormat.parse(duration));
		addEvent(event);
	}

	@Test
	public void testEqualsConstraint() {
		addConstraint("%s:%s", Event.DURATION, "4h");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testRangeConstraint() {
		addConstraint("%s:%s", Event.DURATION, "[0..1d 1h]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalConstraint() {
		addConstraint("%s:%s", Event.DURATION, "foo");
		System.out.println(DurationFormat.parse("foo"));
		execute();
	}
}
