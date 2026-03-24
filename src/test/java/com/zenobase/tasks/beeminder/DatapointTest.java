package com.zenobase.tasks.beeminder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class DatapointTest extends ResultTestSupport {

	@Test
	public void test() {
		assertThat(format(0)).isEqualTo("0:00:00");
		assertThat(format(1000)).isEqualTo("0:00:01");
		assertThat(format(60 * 1000)).isEqualTo("0:01:00");
		assertThat(format(60 * 60 * 1000)).isEqualTo("1:00:00");
		assertThat(format(60 * 60 * 1000 + 2 * 60 * 1000 + 3 * 1000)).isEqualTo("1:02:03");
	}

	private static String format(long d) {
		return Datapoint.formatDuration(BigDecimal.valueOf(d));
	}
}
