package com.zenobase.json;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.testing.NodeAssert;

public class DateTimeRangeFieldTest extends FieldTestSupport {

	private final DateTimeRangeField field = new DateTimeRangeField(FIELD_NAME);

	@Test
	public void testPrePersist() {

		DateTime begin = new DateTime(2014, 11, 2, 22, 5, DateTimeZone.forOffsetHours(-7));
		DateTime end = new DateTime(2014, 11, 3, 8, 10, DateTimeZone.forOffsetHours(-8));
		ObjectNode node = Nodes.newObject();
		field.addValue(node, begin);
		field.addValue(node, end);
		field.prePersist(node);

		NodeAssert nodeAssert = NodeAssert.assertThat(node);
		nodeAssert.path(FIELD_NAME).hasSize(2);
		nodeAssert.path(FIELD_NAME + "$min").isEqualTo("2014-11-02T22:05:00.000-07:00");
		nodeAssert.path(FIELD_NAME + "$max").isEqualTo("2014-11-03T08:10:00.000-08:00");
	}
}
