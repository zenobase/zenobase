package com.zenobase.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import com.zenobase.testing.NodeAssert;

public class DateTimeFieldInternalsTest {

	private final String fieldName = "field";
	private final DateTimeRangeField field = new DateTimeRangeField(fieldName);

	@Test
	public void testLocal() {

		DateTime timestamp = new DateTime(2013, 10, 6, 11, 59, DateTimeZone.forOffsetHours(-7));
		ObjectNode node = Nodes.newObject();
		field.setValue(node, timestamp);
		field.prePersist(node);

		NodeAssert internal = NodeAssert.assertThat(node).path("$" + fieldName).path(0);
		internal.isObject();
		internal.path("time").isEqualTo("2013-10-06T11:59:00.000Z");
		internal.path("month_of_year").isEqualTo(10);
		internal.path("day_of_week").isEqualTo(7);
		internal.path("day_of_month").isEqualTo(6);
		internal.path("day_of_year").isEqualTo(279);
		internal.path("hour_of_day").isEqualTo(11);
	}

	@Test
	public void testMinMax() {

		DateTime begin = new DateTime(2014, 11, 2, 22, 5, DateTimeZone.forOffsetHours(-7));
		DateTime end = new DateTime(2014, 11, 3, 8, 10, DateTimeZone.forOffsetHours(-8));
		ObjectNode node = Nodes.newObject();
		field.addValue(node, begin);
		field.addValue(node, end);
		field.prePersist(node);

		NodeAssert nodeAssert = NodeAssert.assertThat(node);
		nodeAssert.path(fieldName).hasSize(2);
		nodeAssert.path(fieldName + "$min").isEqualTo("2014-11-02T22:05:00.000-07:00");
		nodeAssert.path(fieldName + "$max").isEqualTo("2014-11-03T08:10:00.000-08:00");
	}
}
