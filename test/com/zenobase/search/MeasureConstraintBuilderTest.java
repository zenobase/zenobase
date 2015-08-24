package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.quantity.Length;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;

public class MeasureConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@Before
	public void addEvents() {
		addEvent("0 km");
		addEvent("4 km");
		addEvent("4000 m");
		addEvent("25 km");
		addEvent((String) null);
	}

	private void addEvent(String distance) {
		Event event = new Event();
		if (distance != null) {
			event.setValue(Event.DISTANCE, Measures.<Length>valueOf(distance));
		}
		addEvent(event);
	}

	@Test
	public void testExistsConstraint() {
		addConstraint("%s:*", Event.DISTANCE);
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
	}

	@Test
	public void testEqualsConstraint() {
		addConstraint("%s:%s", Event.DISTANCE, "4 km");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testUnitEqualsConstraint() {
		addConstraint("%s.unit:%s", Event.DISTANCE, "km");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testRangeConstraint() {
		addConstraint("%s:%s", Event.DISTANCE, "[0 km..4 km]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalContraint() {
		addConstraint("%s:%s", Event.DISTANCE, "foo");
		execute();
	}
}
