package com.zenobase.tasks.automatic;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class VehicleResultTest extends ResultTestSupport {

	@Test
	public void test() {
		VehicleResult result = new VehicleResult(readObject("VehicleResultTest.json"));
		assertThat(result.getDisplayName()).isEqualTo("My Speed Demon");
	}
}
