package com.zenobase.search.facets;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.models.Event;
import com.zenobase.search.Search;
import com.zenobase.testing.NodeAssert;
import javax.measure.DecimalMeasure;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StatsFacetTest extends FacetTestSupport {

	private Event first, last;

	@BeforeEach
	@Override
	public void setUp() {
		super.setUp();
		first = newEvent("2 km", 2000);
		last = newEvent("5 km", 5000);
	}

	private static Event newEvent(String length, int count) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.now());
		event.setValue(Event.DISTANCE, DecimalMeasure.valueOf(length));
		event.setValue(Event.COUNT, count);
		return event;
	}

	@Test
	public void testWithNumericField() {
		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,field:%s", FACET_ID, StatsFacet.TYPE, "count");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID);
		node.path("count").isEqualTo(2);
		node.path("min").isEqualTo(2000.0);
		node.path("max").isEqualTo(5000.0);
		node.path("avg").isEqualTo(3500.0);
		node.path("sum").isEqualTo(7000.0);
		node.path("stdev").isEqualTo(1500.0);
	}

	@Test
	public void testWithNumericFieldFiltered() {
		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,field:%s,filter:%s", FACET_ID, StatsFacet.TYPE, "count", "count:(*..3000)");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID);
		node.path("count").isEqualTo(1);
		node.path("min").isEqualTo(2000.0);
		node.path("max").isEqualTo(2000.0);
		node.path("avg").isEqualTo(2000.0);
		node.path("sum").isEqualTo(2000.0);
		node.path("stdev").isEqualTo(0.0);
	}

	@Test
	public void testWithMeasureField() {
		addEvent(first);
		addEvent(last);
		addFacet("id:%s,type:%s,field:%s,unit:%s", FACET_ID, StatsFacet.TYPE, "distance", "m");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
		NodeAssert node = assertThat(result).path(FACET_ID);
		node.path("count").isEqualTo(2);
		node.path("min").isEqualTo(2000.0, "m");
		node.path("max").isEqualTo(5000.0, "m");
		node.path("avg").isEqualTo(3500.0, "m");
		node.path("sum").isEqualTo(7000.0, "m");
		node.path("stdev").isEqualTo(1500.0, "m");
	}

	@Test
	public void testEmpty() {
		addFacet("id:%s,type:%s,field:%s", FACET_ID, StatsFacet.TYPE, "count");

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		NodeAssert node = assertThat(result).path(FACET_ID);
		node.path("count").isEqualTo(0);
		node.path("min").isMissingNode();
	}
}
