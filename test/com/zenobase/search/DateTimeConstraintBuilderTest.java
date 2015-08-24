package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;

public class DateTimeConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@Before
	public void addEvents() {
		addEvent("2012-01-05T12:00:00Z");
		addEvent("2012-01-05T12:15:00Z");
		addEvent("2012-01-05T05:00:00-08:00");
		addEvent((String) null);
	}

	private void addEvent(String timestamp) {
		Event event = new Event();
		if (timestamp != null) {
			event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		}
		addEvent(event);
	}

	@Test
	public void testExistsConstraint() {
		addConstraint("%s:*", Event.TIMESTAMP);
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testEqualsConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "2012-01-05T12:00:00.000Z");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testLocalEqualsConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "2012-01-05T05:00:00.000");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testMillisEqualsConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "1325764800000");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testYearConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "2012TZ");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testLocalYearConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "2012");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testHourConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "2012-01-05T12Z");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testLocalHourConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "2012-01-05T05");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testUnboundedRangeConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "[2012-01-05T13Z..*)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testUnboundedLocalRangeConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "[2012-01-05T06..*)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testOpenRangeConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "(2012-01-05T12Z..2012-01-06TZ)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testClosedRangeConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "(2012-01-05T12Z..2012-01-05TZ]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testOpenLocalRangeConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "(2012-01-05T00..2012-01-05T12)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(1);
	}

	@Test
	public void testClosedLocalRangeConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "(2012-01-05T05..2012-01-05T12]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testRelativeRangeConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "(*..-1M)");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(3);
	}

	@Test
	public void testMillisRangeConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "(1325764800000..1325808000000]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}
}
