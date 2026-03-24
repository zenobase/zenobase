package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import org.junit.Before;
import org.junit.Test;

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
		// Use a bounding box slightly larger than the exact coordinates, because
		// OpenSearch's geo_point encoding may shift points by a tiny amount,
		// causing exact-boundary matches to be unreliable.
		String bounds = Joiner.on(',').join(
			"25.0",
			"-123.0",
			"48.0",
			"-80.0");
		addConstraint("%s:%s", Event.LOCATION, bounds);
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}
}
