package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class DecimalRangeParserTest {

	private final DecimalRangeParser parser = new DecimalRangeParser();

	@Test
	public void testClosedRange() {
		testRange("[-3.1415..1]", Range.closed(new BigDecimal("-3.1415"), BigDecimal.ONE));
	}

	@Test
	public void testOpenRange() {
		testRange("(-3.1415..1)", Range.open(new BigDecimal("-3.1415"), BigDecimal.ONE));
	}

	@Test
	public void testOpenClosedRange() {
		testRange("(-3.1415..1]", Range.openClosed(new BigDecimal("-3.1415"), BigDecimal.ONE));
	}

	@Test
	public void testClosedUnboundedRange() {
		testRange("[-3.1415..*)", Range.downTo(new BigDecimal("-3.1415"), BoundType.CLOSED));
	}

	@Test
	public void testUnboundedClosedRange() {
		testRange("(*..1]", Range.upTo(BigDecimal.ONE, BoundType.CLOSED));
	}

	@Test
	public void testOpenUnboundedRange() {
		testRange("(-3.1415..*)", Range.downTo(new BigDecimal("-3.1415"), BoundType.OPEN));
	}

	@Test
	public void testSingletonRange() {
		testRange("[1..1]", Range.singleton(BigDecimal.ONE));
	}

	@Test
	public void testUnparsedRange() {
		testRange("{-3.1415..1}", null);
		testRange("(*..*)", null);
	}

	@Test
	public void testInvertedRange() {
		assertThatThrownBy(() -> testRange("[1..-3.1415]", null)).isInstanceOf(IllegalArgumentException.class);
	}

	private void testRange(String value, Range<BigDecimal> expected) {
		assertThat(parser.parse(value)).isEqualTo(expected);
	}
}
