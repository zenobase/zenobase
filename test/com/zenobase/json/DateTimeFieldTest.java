package com.zenobase.json;

import com.zenobase.json.DateTimeField;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

public class DateTimeFieldTest extends FieldTestSupport {

	private final DateTimeField field = new DateTimeField(FIELD_NAME);

	@Test
	public void test() {
		roundtrip(field, new DateTime(DateTimeZone.UTC));
		roundtrip(field, new DateTime(DateTimeZone.forOffsetHours(-7)));
	}
}
