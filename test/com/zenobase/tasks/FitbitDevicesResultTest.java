package com.zenobase.tasks;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.LocalDate;
import org.junit.Test;

import com.zenobase.json.Nodes;

public class FitbitDevicesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		FitbitDevicesResult result = new FitbitDevicesResult(readArray("FitbitDevicesResultTest.json"));
		assertThat(result.getLastDate()).as("last sync time").isEqualTo(LocalDate.parse("2012-11-30"));
	}

	@Test
	public void testEmpty() {
		FitbitDevicesResult result = new FitbitDevicesResult(Nodes.newArray());
		assertThat(result.getLastDate()).as("last sync time").isNull();
	}
}
