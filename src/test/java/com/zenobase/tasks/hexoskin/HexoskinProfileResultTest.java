package com.zenobase.tasks.hexoskin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class HexoskinProfileResultTest extends ResultTestSupport {

	@Test
	public void test() {
		HexoskinProfileResult result = new HexoskinProfileResult(readObject("HexoskinProfileResultTest.json"));
		assertThat(result.isMetric()).isFalse();
	}
}
