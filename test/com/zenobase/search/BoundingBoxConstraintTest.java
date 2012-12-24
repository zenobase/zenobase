package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import com.google.common.base.Joiner;

import com.zenobase.models.Event;
import com.zenobase.models.Location;

public class BoundingBoxConstraintTest extends SearchTestSupport {

	private static final Location SEATTLE = new Location("47.6097", "-122.3331");
	private static final Location MIAMI = new Location("25.7878", "-80.2242");
	private static final Location PARIS = new Location("48.8742", "2.3470");

	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {

		super.setUp();

		e1 = new Event();
		e1.setValue(Event.LOCATION, SEATTLE);

		e2 = new Event();
		e2.setValue(Event.LOCATION, MIAMI);

		e3 = new Event();
		e3.setValue(Event.LOCATION, PARIS);
	}

	@Test
	public void test() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFilter(String.format("%s:%s", Event.LOCATION, Joiner.on(',').join(MIAMI.getLatitude(), SEATTLE.getLongitude(), SEATTLE.getLatitude(), MIAMI.getLongitude())));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}
}
