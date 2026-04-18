package com.zenobase.search.constraints;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.models.Event;
import com.zenobase.models.Location;
import com.zenobase.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DistanceConstraintBuilderTest extends ConstraintBuilderTestSupport {

	private static final Location LAS_VEGAS = new Location("36.08", "-115.17");
	private static final Location SAN_DIEGO = new Location("32.82", "-117.13");
	private static final Location DENVER = new Location("39.75", "-104.87");

	@BeforeEach
	public void addEvents() {
		addEvent(LAS_VEGAS);
		addEvent(LAS_VEGAS);
		addEvent(SAN_DIEGO);
		addEvent(DENVER);
	}

	private void addEvent(Location location) {
		Event event = new Event();
		event.setValue(Event.LOCATION, location);
		addEvent(event);
	}

	@Test
	public void testShortDistance() {
		addConstraint("%s:%s", Event.LOCATION, LAS_VEGAS); // location:-115.17,36.08
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testMediumDistance() {
		addConstraint("%s:%s~%s", Event.LOCATION, LAS_VEGAS, "300 mi"); // location:-115.17,36.08~300 mi
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testBadDistance() {
		assertThatThrownBy(() -> {
			addConstraint("%s:%s~%s", Event.LOCATION, LAS_VEGAS, "x"); // location:-115.17,36.08~x
			execute();
		}).isInstanceOf(NumberFormatException.class);
	}
}
