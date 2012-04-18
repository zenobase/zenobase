package com.zenobase.search;

import org.joda.time.DateTimeZone;
import org.junit.Test;

public class TimelineWidgetTest {

	@Test
	public void test() {
		new TimelineWidget("a", "timestamp", "month", null, DateTimeZone.UTC);
	}
}
