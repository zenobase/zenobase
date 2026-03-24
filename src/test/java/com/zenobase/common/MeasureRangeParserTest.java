package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import javax.measure.Measurable;

import com.google.common.collect.Range;
import org.junit.Test;

public class MeasureRangeParserTest {

	private final MeasureRangeParser parser = new MeasureRangeParser();

	@Test
	public void testClosedOpenRange() {
		Measurable<?> lower = Measures.valueOf(new BigDecimal(-1), "mg/L");
		Measurable<?> upper = Measures.valueOf(new BigDecimal(1), "mg/L");
		testRange("[-1 mg/L..1 mg/L)", Range.closedOpen(lower, upper));
	}

	private void testRange(String value, Range<Measurable<?>> expected) {
		assertThat(parser.parse(value)).isEqualTo(expected);
	}
}
