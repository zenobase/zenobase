package com.zenobase.tasks.netatmo;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.google.common.collect.Iterables;

import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class DevicesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		DevicesResult result = new DevicesResult(readObject("DevicesResultTest.json"));
		Device device = Iterables.getOnlyElement(result.getDevices());
		assertThat(device.getId()).as("id").isEqualTo("70:ee:50:ff:80:ee");
		assertThat(device.getCreated()).as("created").isEqualTo(new DateTime("2013-03-28T23:40:07.000Z", DateTimeZone.UTC));
		assertThat(device.getUpdated()).as("updated").isEqualTo(new DateTime("2013-04-03T04:27:16.000Z", DateTimeZone.forID("America/Los_Angeles")));
		assertThat(device.getLocation()).as("location").isEqualTo(new Location("47.6097", "-122.3331"));
	}
}
