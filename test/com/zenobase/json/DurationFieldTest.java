package com.zenobase.json;

import org.joda.time.Duration;
import org.junit.Test;

public class DurationFieldTest extends FieldTestSupport<Duration> {

	@Override
	protected Field<Duration> newField(String name) {
		return new DurationField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(Duration.ZERO);
		roundtrip(new Duration(1000));
	}
}
