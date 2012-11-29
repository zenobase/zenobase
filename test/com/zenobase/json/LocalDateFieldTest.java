package com.zenobase.json;

import org.joda.time.LocalDate;
import org.junit.Test;

public class LocalDateFieldTest extends FieldTestSupport {

	private final LocalDateField field = new LocalDateField(FIELD_NAME);

	@Test
	public void test() {
		roundtrip(field, LocalDate.parse("2012-01-31"));
	}
}
