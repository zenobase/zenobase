package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;

public class TimestampConstraintTest extends ConstraintBuilderTestSupport {

	@Before
	public void addEvents() {
		addEvent("2013-01-17T05:00:00.000Z");
		addEvent("2013-01-17T15:00:00.000Z");
		addEvent("2013-01-18T10:00:00.000Z");
	}

	private void addEvent(String timestamp) {
		addEvent(new Event(), DateTime.parse(timestamp));
	}

	@Test
	public void testEquals() {
		addConstraint("%s:%s", "_timestamp", "2013-01-17T15:00:00.000Z");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testRange() {
		addConstraint("%s:%s", "_timestamp", "[2013-01-17T05:00:00.000Z..2013-01-18T10:00:00.000Z)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testEmpty() {
		addConstraint("%s:%s", "_timestamp", "2013-01-19T00:00:00.000Z");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(0);
	}
}
