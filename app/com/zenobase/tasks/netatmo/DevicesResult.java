package com.zenobase.tasks.netatmo;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.models.Location;

class DevicesResult {

	private final JsonNode node;

	public DevicesResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public boolean isSuccess() {
		return "ok".equals(node.path("status").textValue());
	}

	public List<Device> getDevices() {
		Preconditions.checkState(isSuccess(), "Expected a successful response but got <%s>", node);
		List<Device> devices = Lists.newArrayList();
		for (JsonNode item : node.path("body").path("devices")) {
			devices.add(getDevice(item));
		}
		return devices;
	}

	private static Device getDevice(JsonNode node) {
		String id = node.path("_id").textValue();
		String label = node.path("module_name").textValue();
		DateTime created = getTimestamp(node.path("date_setup").path("sec"), DateTimeZone.UTC);
		DateTime updated = getTimestamp(node.path("last_status_store"), getTimezone(node.path("place").path("timezone")));
		Location location = getLocation(node.path("place").path("location"));
		return new Device(id, label, created, updated, location);
	}

	private static DateTimeZone getTimezone(JsonNode node) {
		Preconditions.checkArgument(node.isTextual(),
			"Expected text but got <%s>", node);
		return DateTimeZone.forID(node.textValue());
	}

	private static DateTime getTimestamp(JsonNode node, DateTimeZone timezone) {
		Preconditions.checkArgument(node.longValue() != 0,
			"Expected a number but got <%s>", node);
		return new DateTime(node.longValue() * 1000, timezone);
	}

	private static Location getLocation(JsonNode node) {
		Preconditions.checkArgument(node.isArray() && node.size() == 2,
			"Expected an array with two elements but got <%s>", node);
		return new Location(node.path(0).decimalValue(), node.path(1).decimalValue());
	}
}
