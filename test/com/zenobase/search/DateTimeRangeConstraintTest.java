package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.Event;

public class DateTimeRangeConstraintTest extends ConstraintTestSupport {

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
	public void test() {
		addFilter("%s:%s", Event.TIMESTAMP, "2012-01-05T12+0000");
		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}
}
