package com.zenobase.tasks.netatmo;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collection;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.google.common.collect.Iterables;

import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class DevicesResultTest extends ResultTestSupport {

	@Test
	public void test() {
		DevicesResult result = new DevicesResult(readObject("DevicesResultTest.json"), true);
		Collection<Device> devices = result.getDevices();
		assertThat(devices).hasSize(2);

		Device dev0 = Iterables.get(devices, 0);
		assertThat(dev0.getId()).as("id").isEqualTo("70:ee:50:00:80:ee");
		assertThat(dev0.getLabel()).as("label").isEqualTo("Bedroom");
		assertThat(dev0.getCreated()).as("created").isEqualTo(new DateTime("2013-03-28T23:40:07.000Z", DateTimeZone.UTC));
		assertThat(dev0.getUpdated()).as("updated").isEqualTo(new DateTime("2013-04-03T04:27:16.000Z", DateTimeZone.forID("America/Los_Angeles")));
		assertThat(dev0.getLocation()).as("location").isEqualTo(new Location("47.6097", "-122.3331"));

		Device dev1 = Iterables.get(devices, 1);
		assertThat(dev1.getId()).as("id").isEqualTo("02:00:00:00:7b:a2");
		assertThat(dev1.getLabel()).as("label").isEqualTo("Bathroom");
		assertThat(dev1.getCreated()).as("created").isEqualTo(new DateTime("2013-03-28T23:40:07.000Z", DateTimeZone.UTC));
		assertThat(dev1.getUpdated()).as("updated").isEqualTo(new DateTime("2013-04-03T04:27:16.000Z", DateTimeZone.forID("America/Los_Angeles")));
		assertThat(dev1.getLocation()).as("location").isEqualTo(new Location("47.6097", "-122.3331"));
	}

	@Test
	public void testNoIncludeModules() {
		DevicesResult result = new DevicesResult(readObject("DevicesResultTest.json"), false);
		Device device = Iterables.getOnlyElement(result.getDevices());
		assertThat(device.getId()).as("id").isEqualTo("70:ee:50:00:80:ee");
	}
}
