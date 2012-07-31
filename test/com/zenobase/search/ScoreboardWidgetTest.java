package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Event;
import com.zenobase.testing.NodeAssert;

public class ScoreboardWidgetTest extends SearchTestSupport {

	private String id = Generator.id();
	private Event e1, e2, e3, e4;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		e1 = newEvent("walk", new DecimalMeasure<Length>(new BigDecimal(10), SI.KILOMETER));
		e2 = newEvent("hike", new DecimalMeasure<Length>(new BigDecimal(10000), SI.METER));
		e3 = newEvent("hike", new DecimalMeasure<Length>(new BigDecimal(20), SI.KILOMETER));
		e4 = newEvent("climb", null);
	}

	private static Event newEvent(String tag, DecimalMeasure<Length> distance) {
		Event event = new Event();
		event.setValue(Event.TAG, tag);
		event.setValue(Event.DISTANCE, distance);
		return event;
	}

	@Test
	public void testDefault() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addEvent(e4);
		addWidget(String.format("id:%s,type:%s,termField:%s,valueField:%s,unit:%s,order:%s", id, ScoreboardWidget.TYPE, Event.TAG, Event.DISTANCE, "km", "total"));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(4);
		NodeAssert node = assertThat(result).path(id).hasSize(2);
		node.path(0).path("label").isEqualTo(e2.getValue(Event.TAG));
		node.path(0).path("min").path("@value").isEqualTo(10);
		node.path(0).path("min").path("unit").isEqualTo("km");
		node.path(0).path("max").path("@value").isEqualTo(20);
		node.path(0).path("max").path("unit").isEqualTo("km");
		node.path(0).path("avg").path("@value").isEqualTo(15);
		node.path(0).path("avg").path("unit").isEqualTo("km");
		node.path(0).path("sum").path("@value").isEqualTo(30);
		node.path(0).path("sum").path("unit").isEqualTo("km");
		node.path(0).path("count").isEqualTo(2);
		node.path(1).path("label").isEqualTo(e1.getValue(Event.TAG));
		node.path(1).path("min").path("@value").isEqualTo(10);
		node.path(1).path("max").path("@value").isEqualTo(10);
		node.path(1).path("avg").path("@value").isEqualTo(10);
		node.path(1).path("sum").path("@value").isEqualTo(10);
		node.path(1).path("count").isEqualTo(1);
	}

	@Test
	public void testEmpty() {

		addWidget(String.format("id:%s,type:%s,termField:%s,valueField:%s,unit:%s,order:%s", id, ScoreboardWidget.TYPE, Event.TAG, Event.DISTANCE, "km", "total"));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(0);
		assertThat(result).path(id).hasSize(0);
	}
}
