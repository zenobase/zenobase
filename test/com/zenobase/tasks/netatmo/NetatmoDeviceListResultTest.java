package com.zenobase.tasks.netatmo;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.google.common.collect.Iterables;

import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class NetatmoDeviceListResultTest extends ResultTestSupport {

	@Test
	public void test() {
		NetatmoDeviceListResult result = new NetatmoDeviceListResult(readObject("NetatmoDeviceListResultTest.json"));
		assertThat(result.getStatus()).as("status").isEqualTo("ok");
		Device device = Iterables.getOnlyElement(result.getDevices());
		assertThat(device.getId()).as("id").isEqualTo("70:ee:50:ff:80:ee");
		assertThat(device.getTimestamp()).as("timestamp").isEqualTo(new DateTime("2013-04-03T04:27:16.000Z", DateTimeZone.forID("America/Los_Angeles")));
		assertThat(device.getLocation()).as("location").isEqualTo(new Location("-122.3331", "47.6097"));
	}
}
