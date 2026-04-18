package com.zenobase.search.constraints;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.common.DurationFormat;
import com.zenobase.models.Event;
import com.zenobase.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DurationConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@BeforeEach
	public void addEvents() {
		addEvent("0s");
		addEvent("4h");
		addEvent("240min");
		addEvent("1d 1h");
		addEvent((String) null);
	}

	private void addEvent(String duration) {
		Event event = new Event();
		if (duration != null) {
			event.setValue(Event.DURATION, DurationFormat.parse(duration));
		}
		addEvent(event);
	}

	@Test
	public void testExistsConstraint() {
		addConstraint("%s:*", Event.DURATION);
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
	}

	@Test
	public void testEqualsConstraint() {
		addConstraint("%s:%s", Event.DURATION, "4h");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testRangeConstraint() {
		addConstraint("%s:%s", Event.DURATION, "[0..1d 1h]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
	}

	@Test
	public void testIllegalConstraint() {
		assertThatThrownBy(() -> {
			addConstraint("%s:%s", Event.DURATION, "foo");
			execute();
		}).isInstanceOf(IllegalArgumentException.class);
	}
}
