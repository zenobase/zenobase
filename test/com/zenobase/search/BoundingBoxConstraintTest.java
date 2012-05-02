package com.zenobase.search;

import static com.zenobase.test.NodeAssert.assertThat;

import java.math.BigDecimal;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import com.google.common.base.Joiner;

import com.zenobase.models.Event;
import com.zenobase.models.Location;

public class BoundingBoxConstraintTest extends WidgetTestSupport {

	private Event e1, e2, e3;
	private Location seattle = new Location(new BigDecimal("47.6097"), new BigDecimal("-122.3331"));
	private Location miami = new Location(new BigDecimal("25.7878"), new BigDecimal("-80.2242"));
	private Location paris = new Location(new BigDecimal("48.8742"), new BigDecimal("2.3470"));

	@Before
	@Override
	public void setUp() {

		super.setUp();

		e1 = new Event();
		e1.setValue(Event.LOCATION, seattle);

		e2 = new Event();
		e2.setValue(Event.LOCATION, miami);

		e3 = new Event();
		e3.setValue(Event.LOCATION, paris);
	}

	@Test
	public void test() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFilter(String.format("%s:%s", Event.LOCATION, Joiner.on(',').join(miami.getLatitude(), seattle.getLongitude(), seattle.getLatitude(), miami.getLongitude())));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}
}
