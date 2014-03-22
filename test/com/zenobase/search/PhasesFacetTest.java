package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import java.math.BigDecimal;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Phase;
import com.zenobase.testing.NodeAssert;

public class PhasesFacetTest extends FacetTestSupport {

	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent(0.0, 5000.0, 10000);
		e2 = newEvent(0.125, 2000.0, 4000);
		e3 = newEvent(0.126, 3000.0, 6000);
		e4 = newEvent(0.8, 6000.0, 12000);
	}

	private static Event newEvent(double phase, double distance, int steps) {
		Event event = new Event();
		event.setValue(Event.PHASE, Phase.valueOf(phase));
		event.setValue(Event.DISTANCE, Measures.valueOf(BigDecimal.valueOf(distance), SI.METER));
		event.setValue(Event.COUNT, steps);
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s", FACET_ID, PhasesFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(8);
		node.path(0).path("from").isEqualTo(0.0);
		node.path(0).path("to").isEqualTo(0.125);
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("from").isEqualTo(0.125);
		node.path(1).path("to").isEqualTo(0.25);
		node.path(1).path("count").isEqualTo(2);
		node.path(2).path("from").isEqualTo(0.25);
		node.path(2).path("to").isEqualTo(0.375);
		node.path(2).path("count").isEqualTo(0);
		node.path(3).path("from").isEqualTo(0.375);
		node.path(3).path("to").isEqualTo(0.5);
		node.path(3).path("count").isEqualTo(0);
		node.path(4).path("from").isEqualTo(0.5);
		node.path(4).path("to").isEqualTo(0.625);
		node.path(4).path("count").isEqualTo(0);
		node.path(5).path("from").isEqualTo(0.625);
		node.path(5).path("to").isEqualTo(0.75);
		node.path(5).path("count").isEqualTo(0);
		node.path(6).path("from").isEqualTo(0.75);
		node.path(6).path("to").isEqualTo(0.875);
		node.path(6).path("count").isEqualTo(1);
		node.path(7).path("from").isEqualTo(0.875);
		node.path(7).path("to").isEqualTo(1.0);
		node.path(7).path("count").isEqualTo(0);
	}

	@Test
	public void testFiltered() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,filter:%s", FACET_ID, PhasesFacet.TYPE, "phase:[0..0.5)");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(8);
		node.path(0).path("from").isEqualTo(0.0);
		node.path(0).path("to").isEqualTo(0.125);
		node.path(0).path("count").isEqualTo(1);
		node.path(1).path("from").isEqualTo(0.125);
		node.path(1).path("to").isEqualTo(0.25);
		node.path(1).path("count").isEqualTo(2);
		node.path(2).path("from").isEqualTo(0.25);
		node.path(2).path("to").isEqualTo(0.375);
		node.path(2).path("count").isEqualTo(0);
		node.path(3).path("from").isEqualTo(0.375);
		node.path(3).path("to").isEqualTo(0.5);
		node.path(3).path("count").isEqualTo(0);
		node.path(4).path("from").isEqualTo(0.5);
		node.path(4).path("to").isEqualTo(0.625);
		node.path(4).path("count").isEqualTo(0);
		node.path(5).path("from").isEqualTo(0.625);
		node.path(5).path("to").isEqualTo(0.75);
		node.path(5).path("count").isEqualTo(0);
		node.path(6).path("from").isEqualTo(0.75);
		node.path(6).path("to").isEqualTo(0.875);
		node.path(6).path("count").isEqualTo(0);
		node.path(7).path("from").isEqualTo(0.875);
		node.path(7).path("to").isEqualTo(1.0);
		node.path(7).path("count").isEqualTo(0);
	}

	@Test
	public void testTwoPhases() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,phases:%d", FACET_ID, PhasesFacet.TYPE, 2);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("from").isEqualTo(0.0);
		node.path(0).path("to").isEqualTo(0.5);
		node.path(0).path("count").isEqualTo(3);
		node.path(1).path("from").isEqualTo(0.5);
		node.path(1).path("to").isEqualTo(1.0);
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testMeasureField() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addFacet("id:%s,type:%s,value_field:%s,unit:%s", FACET_ID, PhasesFacet.TYPE, "distance", "m");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(8);
		node.path(0).path("from").isEqualTo(0.0);
		node.path(0).path("to").isEqualTo(0.125);
		node.path(0).path("count").isEqualTo(1);
		node.path(0).path("min").isEqualTo(5000.0, "m");
		node.path(0).path("max").isEqualTo(5000.0, "m");
		node.path(0).path("avg").isEqualTo(5000.0, "m");
		node.path(1).path("from").isEqualTo(0.125);
		node.path(1).path("to").isEqualTo(0.25);
		node.path(1).path("count").isEqualTo(2);
		node.path(1).path("min").isEqualTo(2000.0, "m");
		node.path(1).path("max").isEqualTo(3000.0, "m");
		node.path(1).path("avg").isEqualTo(2500.0, "m");
		node.path(2).path("from").isEqualTo(0.25);
		node.path(2).path("to").isEqualTo(0.375);
		node.path(2).path("count").isEqualTo(0);
		node.path(3).path("from").isEqualTo(0.375);
		node.path(3).path("to").isEqualTo(0.5);
		node.path(3).path("count").isEqualTo(0);
		node.path(4).path("from").isEqualTo(0.5);
		node.path(4).path("to").isEqualTo(0.625);
		node.path(4).path("count").isEqualTo(0);
		node.path(5).path("from").isEqualTo(0.625);
		node.path(5).path("to").isEqualTo(0.75);
		node.path(5).path("count").isEqualTo(0);
		node.path(6).path("from").isEqualTo(0.75);
		node.path(6).path("to").isEqualTo(0.875);
		node.path(6).path("count").isEqualTo(1);
		node.path(6).path("min").isEqualTo(6000.0, "m");
		node.path(6).path("max").isEqualTo(6000.0, "m");
		node.path(6).path("avg").isEqualTo(6000.0, "m");
		node.path(7).path("from").isEqualTo(0.875);
		node.path(7).path("to").isEqualTo(1.0);
		node.path(7).path("count").isEqualTo(0);
	}

	@Test
	@Ignore
	public void testNumericField() {

		addFacet("id:%s,type:%s,value_field:%s", FACET_ID, PhasesFacet.TYPE, "count");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(8);
		node.path(0).path("from").isEqualTo(0.0);
		node.path(0).path("to").isEqualTo(0.125);
		node.path(0).path("count").isEqualTo(1);
		node.path(0).path("min").isEqualTo(10000);
		node.path(0).path("max").isEqualTo(10000.0);
		node.path(0).path("avg").isEqualTo(10000.0);
		node.path(1).path("from").isEqualTo(0.125);
		node.path(1).path("to").isEqualTo(0.25);
		node.path(1).path("count").isEqualTo(2);
		node.path(1).path("min").isEqualTo(4000);
		node.path(1).path("max").isEqualTo(6000);
		node.path(1).path("avg").isEqualTo(5000);
		node.path(2).path("from").isEqualTo(0.25);
		node.path(2).path("to").isEqualTo(0.375);
		node.path(2).path("count").isEqualTo(0);
		node.path(3).path("from").isEqualTo(0.375);
		node.path(3).path("to").isEqualTo(0.5);
		node.path(3).path("count").isEqualTo(0);
		node.path(4).path("from").isEqualTo(0.5);
		node.path(4).path("to").isEqualTo(0.625);
		node.path(4).path("count").isEqualTo(0);
		node.path(5).path("from").isEqualTo(0.625);
		node.path(5).path("to").isEqualTo(0.75);
		node.path(5).path("count").isEqualTo(0);
		node.path(6).path("from").isEqualTo(0.75);
		node.path(6).path("to").isEqualTo(0.875);
		node.path(6).path("count").isEqualTo(1);
		node.path(6).path("min").isEqualTo(12000);
		node.path(6).path("max").isEqualTo(12000);
		node.path(6).path("avg").isEqualTo(12000);
		node.path(7).path("from").isEqualTo(0.875);
		node.path(7).path("to").isEqualTo(1.0);
		node.path(7).path("count").isEqualTo(0);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s", FACET_ID, PhasesFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
