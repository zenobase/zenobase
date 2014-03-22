package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Event;
import com.zenobase.models.Phase;

public class PhaseConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@Before
	public void addEvents() {
		addEvent(0.0);
		addEvent(0.1);
		addEvent(0.5);
		addEvent(0.9);
	}

	private void addEvent(double phase) {
		Event event = new Event();
		event.setValue(Event.PHASE, Phase.valueOf(phase));
		addEvent(event);
	}

	@Test
	public void testNoneEquals() {
		addConstraint("phase:0.2");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
	}

	@Test
	public void testEquals() {
		addConstraint("phase:0.1");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testEqualsWithRemainder() {
		addConstraint("phase:1.1");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testRange() {
		addConstraint("phase:[0.1..0.9)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	@Ignore
	public void testRangeWithRemainder() {
		addConstraint("phase:[0.9..1.1]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}
}
