package com.zenobase.search.constraints;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.models.Event;
import com.zenobase.models.Rating;
import com.zenobase.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DecimalRangeConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@BeforeEach
	public void addEvents() {
		addEvent(0);
		addEvent(40);
		addEvent(40);
		addEvent(100);
	}

	private void addEvent(int rating) {
		Event event = new Event();
		event.setValue(Event.RATING, Rating.valueOf(rating));
		addEvent(event);
	}

	@Test
	public void testRange() {
		addConstraint("%s:%s", Event.RATING, "[0..100]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(4);
	}

	@Test
	public void testEmptyRange() {
		addConstraint("%s:%s", Event.RATING, "(0..40)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
	}

	@Test
	public void testLowerRange() {
		addConstraint("%s:%s", Event.RATING, "(*..50]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testUpperRange() {
		addConstraint("%s:%s", Event.RATING, "[50..*)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testInvalidRange() {
		assertThatThrownBy(() -> {
			addConstraint("%s:%s", Event.RATING, "[100..0]");
			execute();
		}).isInstanceOf(IllegalArgumentException.class);
	}
}
