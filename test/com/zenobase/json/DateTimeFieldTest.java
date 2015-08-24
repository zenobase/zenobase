package com.zenobase.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.testing.NodeAssert;

public class DateTimeFieldTest extends FieldTestSupport<DateTime> {

	@Override
	protected Field<DateTime> newField(String name) {
		return new DateTimeField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(new DateTime(DateTimeZone.UTC));
		roundtrip(new DateTime(DateTimeZone.forOffsetHours(-7)));
	}

	@Test
	public void testPrePersist() {
		DateTimeField field = new DateTimeField("field");
		DateTime timestamp = new DateTime(2013, 10, 6, 11, 59, DateTimeZone.forOffsetHours(-7));
		ObjectNode node = Nodes.newObject();
		field.setValue(node, timestamp);
		field.prePersist(node);

		NodeAssert internal = NodeAssert.assertThat(node).path("$field").path(0);
		internal.isObject();
		internal.path("time").isEqualTo("2013-10-06T11:59:00.000Z");
		internal.path("month_of_year").isEqualTo(10);
		internal.path("day_of_week").isEqualTo(7);
		internal.path("day_of_month").isEqualTo(6);
		internal.path("day_of_year").isEqualTo(279);
		internal.path("hour_of_day").isEqualTo(11);
	}
}
