package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import com.google.common.base.Joiner;

import com.zenobase.models.Event;
import com.zenobase.models.Location;

public class BoundingBoxConstraintBuilderTest extends ConstraintBuilderTestSupport {

	private static final Location SEATTLE = new Location("47.6097", "-122.3331");
	private static final Location MIAMI = new Location("25.7878", "-80.2242");
	private static final Location PARIS = new Location("48.8742", "2.3470");

	@Before
	public void addEvents() {
		addEvent(SEATTLE);
		addEvent(MIAMI);
		addEvent(PARIS);
	}

	private void addEvent(Location location) {
		Event event = new Event();
		event.setValue(Event.LOCATION, location);
		addEvent(event);
	}

	@Test
	public void test() {
		String bounds = Joiner.on(',').join(
			MIAMI.getLatitude(),
			SEATTLE.getLongitude(),
			SEATTLE.getLatitude(),
			MIAMI.getLongitude());
		addConstraint("%s:%s", Event.LOCATION, bounds);
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}
}
