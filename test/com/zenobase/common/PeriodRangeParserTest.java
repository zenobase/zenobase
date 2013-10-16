package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import com.google.common.collect.Range;

public class PeriodRangeParserTest {

	private final PeriodRangeParser parser = new PeriodRangeParser();

	@Test
	public void test() {
		test("[-1y..+1y]", Range.closed(StandardPeriod.valueOf("-1y"), StandardPeriod.valueOf("+1y")));
		test("[-1y+2M..+3y-4h]", Range.closed(StandardPeriod.valueOf("-1y+2M"), StandardPeriod.valueOf("+3y-4h")));
	}

	public void test(String s, Range<StandardPeriod> expected) {
		assertThat(parser.parse(s)).isEqualTo(expected);
	}
}
