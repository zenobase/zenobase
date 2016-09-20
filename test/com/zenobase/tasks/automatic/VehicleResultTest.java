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

    @Test
    public void testVehicleNoDisplayName() {
        VehicleResult result = new VehicleResult(readObject("VehicleResultTest-NoDisplayName.json"));
        assertThat(result.getDisplayName()).isEqualTo("2001 Acura MDX");
    }

    @Test
    public void testVehicleNoDisplayNameNoYear() {
        VehicleResult result = new VehicleResult(readObject("VehicleResultTest-NoDisplayNameNoYear.json"));
        assertThat(result.getDisplayName()).isEqualTo("Acura MDX");
    }

    @Test
    public void testVehicleNoNothing() {
        VehicleResult result = new VehicleResult(readObject("VehicleResultTest-NoNothing.json"));
        assertThat(result.getDisplayName()).isNull();
    }
}
