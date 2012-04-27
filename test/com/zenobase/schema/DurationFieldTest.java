package com.zenobase.schema;

import org.joda.time.Duration;
import org.junit.Test;

public class DurationFieldTest extends FieldTestSupport {

	private final DurationField field = new DurationField(FIELD_NAME);

	@Test
	public void test() {
		roundtrip(field, Duration.ZERO);
		roundtrip(field, new Duration(1000));
	}
}
