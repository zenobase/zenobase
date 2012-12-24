package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.Test;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

public class DecimalRangeParserTest {

	private final DecimalRangeParser parser = new DecimalRangeParser();

	@Test
	public void testClosedRange() {
		testRange("[-3.1415..1]", Ranges.closed(new BigDecimal("-3.1415"), BigDecimal.ONE));
	}

	@Test
	public void testOpenRange() {
		testRange("(-3.1415..1)", Ranges.open(new BigDecimal("-3.1415"), BigDecimal.ONE));
	}

	@Test
	public void testOpenClosedRange() {
		testRange("(-3.1415..1]", Ranges.openClosed(new BigDecimal("-3.1415"), BigDecimal.ONE));
	}

	@Test
	public void testClosedUnboundedRange() {
		testRange("[-3.1415..*)", Ranges.downTo(new BigDecimal("-3.1415"), BoundType.CLOSED));
	}

	@Test
	public void testUnboundedClosedRange() {
		testRange("(*..1]", Ranges.upTo(BigDecimal.ONE, BoundType.CLOSED));
	}

	@Test
	public void testOpenUnboundedRange() {
		testRange("(-3.1415..*)", Ranges.downTo(new BigDecimal("-3.1415"), BoundType.OPEN));
	}

	@Test
	public void testUnboundedRange() {
		testRange("(*..*)", Ranges.<BigDecimal>all());
	}

	@Test
	public void testSingletonRange() {
		testRange("[1..1]", Ranges.singleton(BigDecimal.ONE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMisformattedRange() {
		testRange("{-3.1415..1}", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvertedRange() {
		testRange("[1..-3.1415]", null);
	}

	private void testRange(String value, Range<BigDecimal> expected) {
		assertThat(parser.parse(value)).isEqualTo(expected);
	}
}
