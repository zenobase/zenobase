package com.zenobase.tasks.netatmo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import com.google.common.collect.Iterables;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import com.zenobase.models.Location;
import com.zenobase.tasks.ResultTestSupport;

public class StationsResultTest extends ResultTestSupport {

	@Test
	public void test() {
		StationsResult result = new StationsResult(readObject("StationsResultTest.json"), true);
		Collection<Device> devices = result.getDevices();
		assertThat(devices).hasSize(2);

		Device dev0 = Iterables.get(devices, 0);
		assertThat(dev0.getId()).as("id").isEqualTo("70:ee:50:00:80:ee");
		assertThat(dev0.getModuleId()).as("module id").isNull();
		assertThat(dev0.getLabel()).as("label").isEqualTo("Bedroom");
		assertThat(dev0.getCreated())
				.as("created")
				.isEqualTo(new DateTime("2013-03-28T23:40:07.000Z", DateTimeZone.UTC));
		assertThat(dev0.getUpdated())
				.as("updated")
				.isEqualTo(new DateTime("2016-11-03T18:58:14.000Z", DateTimeZone.forID("America/Los_Angeles")));
		assertThat(dev0.getLocation()).as("location").isEqualTo(new Location("47.6097", "-122.3331"));
		assertThat(dev0.supports("Temperature")).as("supports temperature").isTrue();
		assertThat(dev0.supports("Noise")).as("supports temperature").isTrue();
		assertThat(dev0.supports("Rain")).as("supports rain").isFalse();

		Device dev1 = Iterables.get(devices, 1);
		assertThat(dev1.getId()).as("id").isEqualTo("70:ee:50:00:80:ee");
		assertThat(dev1.getModuleId()).as("module id").isEqualTo("02:00:00:00:7b:a2");
		assertThat(dev1.getLabel()).as("label").isEqualTo("Bathroom");
		assertThat(dev1.getCreated())
				.as("created")
				.isEqualTo(new DateTime("2013-03-28T23:40:07.000Z", DateTimeZone.UTC));
		assertThat(dev1.getUpdated())
				.as("updated")
				.isEqualTo(new DateTime("2016-11-03T18:58:14.000Z", DateTimeZone.forID("America/Los_Angeles")));
		assertThat(dev1.getLocation()).as("location").isEqualTo(new Location("47.6097", "-122.3331"));
		assertThat(dev1.supports("Temperature")).as("supports temperature").isTrue();
		assertThat(dev1.supports("Noise")).as("supports temperature").isFalse();
		assertThat(dev1.supports("Rain")).as("supports rain").isFalse();
	}

	@Test
	public void testNoIncludeModules() {
		StationsResult result = new StationsResult(readObject("StationsResultTest.json"), false);
		Device device = Iterables.getOnlyElement(result.getDevices());
		assertThat(device.getId()).as("id").isEqualTo("70:ee:50:00:80:ee");
	}
}
