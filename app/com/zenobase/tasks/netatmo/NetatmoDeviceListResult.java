package com.zenobase.tasks.netatmo;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.collect.Lists;

import com.zenobase.models.Location;

class NetatmoDeviceListResult {

	private final JsonNode node;

	public NetatmoDeviceListResult(JsonNode node) {
		this.node = node;
	}

	public String getStatus() {
		return node.path("status").getTextValue();
	}

	public List<Device> getDevices() {
		List<Device> devices = Lists.newArrayList();
		for (JsonNode item : node.path("body").path("devices")) {
			devices.add(getDevice(item));
		}
		return devices;
	}

	private Device getDevice(JsonNode node) {
		String id = node.path("_id").getTextValue();
		String label = node.path("module_name").getTextValue();
		DateTimeZone timezone = DateTimeZone.forID(node.path("place").path("timezone").getTextValue());
		DateTime timestamp = new DateTime(node.path("last_status_store").getLongValue() * 1000, timezone);
		Location location = getLocation(node.path("place").path("location"));
		return new Device(id, label, timestamp, location);
	}

	private Location getLocation(JsonNode node) {
		return new Location(node.path(0).getDecimalValue(), node.path(1).getDecimalValue());
	}
}
