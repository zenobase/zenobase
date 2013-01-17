package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;

public class DateTimeConstraintBuilderTest extends ConstraintBuilderTestSupport {

	@Before
	public void addEvents() {
		addEvent("2012-01-05T12:00:00Z");
		addEvent("2012-01-05T12:15:00Z");
		addEvent("2012-01-05T13:00:00Z");
	}

	private void addEvent(String timestamp) {
		Event event = new Event();
		event.setValue(Event.TIMESTAMP, DateTime.parse(timestamp));
		addEvent(event);
	}

	@Test
	public void testEqualsConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "2012-01-05T12:00:00.000Z");
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
	public void testHourConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "2012-01-05T12Z");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testRangeConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "(2012-01-05T12Z..2012-01-06TZ]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}

	@Test
	public void testMillisRangeConstraint() {
		addConstraint("%s:%s", Event.TIMESTAMP, "(1325764800000..1325808000000]");
		ObjectNode result = execute();
		assertThat(result).path(Search.TOTAL.getName()).isEqualTo(2);
	}
}
