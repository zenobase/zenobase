package com.zenobase.search;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.common.Intervals;
import com.zenobase.models.Event;

public class DateTimeRangeConstraintTest extends SearchTestSupport {

	private Event e1, e2, e3;

	@Before
	@Override
	public void setUp() {

		super.setUp();

		e1 = new Event();
		e1.setValue(Event.TIMESTAMP, new DateTime(2012, 1, 5, 12, 0, 0, DateTimeZone.UTC));

		e2 = new Event();
		e2.setValue(Event.TIMESTAMP, new DateTime(2012, 1, 5, 12, 15, 0, DateTimeZone.UTC));

		e3 = new Event();
		e3.setValue(Event.TIMESTAMP, new DateTime(2012, 1, 5, 13, 0, 0, DateTimeZone.UTC));
	}

	@Test
	public void test() {

		addEvent(e1);
		addEvent(e2);
		addEvent(e3);
		addFilter(String.format("%s:%s", Event.TIMESTAMP, Intervals.toString(e1.getValue(Event.TIMESTAMP), "hour")));

		ObjectNode result = execute();
		assertThat(result).path(EventSearch.TOTAL.getName()).isEqualTo(2);
	}
}
