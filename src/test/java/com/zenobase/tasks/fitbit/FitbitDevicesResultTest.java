package com.zenobase.tasks.fitbit;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.json.Nodes;
import com.zenobase.tasks.ResultTestSupport;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

public class FitbitDevicesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		FitbitDevicesResult result = new FitbitDevicesResult(readArray("FitbitDevicesResultTest.json"));
		assertThat(result.getLastDate(DeviceType.TRACKER))
			.as("last tracker sync time")
			.isEqualTo(LocalDate.parse("2012-11-30"));
		assertThat(result.getLastDate(DeviceType.SCALE))
			.as("last scale sync time")
			.isEqualTo(LocalDate.parse("2014-09-30"));
	}

	@Test
	public void testEmpty() {
		FitbitDevicesResult result = new FitbitDevicesResult(Nodes.newArray());
		assertThat(result.getLastDate(DeviceType.TRACKER)).as("last sync time").isNull();
	}
}
