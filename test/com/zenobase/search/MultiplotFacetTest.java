package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class MultiplotFacetTest extends FacetTestSupport {

	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("2012-03-30T08:00:00Z", "4 km", "100 ft", 2000);
		e2 = newEvent("2012-03-30T15:00:00Z", "6 km", "300 ft", 4000);
		e3 = newEvent("2012-04-15T09:00:00Z", "10 km", "400 ft", 5000);
	}

	private static Event newEvent(String timestamp, String distance, String height, int steps) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		event.setValue(Event.DISTANCE, DecimalMeasure.<Length>valueOf(distance));
		event.setValue(Event.HEIGHT, DecimalMeasure.<Length>valueOf(height));
		event.setValue(Event.COUNT, steps);
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFacet("id:%s,type:%s,fields:distance|height|count,units:km|ft|,interval:day,statistic:avg", FACET_ID, MultiplotFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
		NodeAssert node = assertThat(result).path(FACET_ID).hasSize(2);
		node.path(0).path("label").isEqualTo("2012-03-30TZ");
		node.path(0).path("distance").isEqualTo(5.0);
		node.path(0).path("height").isEqualTo(200.0);
		node.path(0).path("count").isEqualTo(3000.0);
		node.path(1).path("label").isEqualTo("2012-04-15TZ");
		node.path(1).path("distance").isEqualTo(10.0);
		node.path(1).path("height").isEqualTo(400.0);
		node.path(1).path("count").isEqualTo(5000.0);
	}

	@Test
	public void testEmpty() {

		addFacet("id:%s,type:%s,fields:distance|count,units:km|", FACET_ID, MultiplotFacet.TYPE);

		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(FACET_ID).hasSize(0);
	}
}
