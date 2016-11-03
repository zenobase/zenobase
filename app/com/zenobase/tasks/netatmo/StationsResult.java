package com.zenobase.tasks.netatmo;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Location;

class StationsResult {

	private final JsonNode node;
	private final boolean includeModules;

	public StationsResult(JsonNode node, boolean includeModules) {
		this.node = Preconditions.checkNotNull(node);
		this.includeModules = includeModules;
	}

	public boolean isSuccess() {
		return "ok".equals(node.path("status").textValue());
	}

	public Collection<Device> getDevices() {
		Preconditions.checkState(isSuccess(), "Expected a successful response but got <%s>", node);
		List<Device> devices = Lists.newArrayList();
		for (JsonNode deviceNode : node.path("body").path("devices")) {
			Device device = parseDevice(deviceNode);
			devices.add(device);
			if (includeModules) {
			    for (JsonNode moduleNode : deviceNode.path("modules")) {
			        devices.add(parseModule(device, moduleNode));
			    }
			}
		}
		return devices;
	}

	private static Device parseDevice(JsonNode node) {
		String id = node.path("_id").textValue();
		String label = node.path("module_name").textValue();
		DateTime created = getTimestamp(node.path("date_setup"), DateTimeZone.UTC);
		DateTime updated = getTimestamp(node.path("last_status_store"), getTimezone(node.path("place").path("timezone")));
		Location location = getLocation(node.path("place").path("location"));
		return new Device(id, label, created, updated, location);
	}

	private static Device parseModule(Device device, JsonNode node) {
	    String moduleId = node.path("_id").textValue();
		String moduleLabel = node.path("module_name").textValue();
		return new Device(device.getId(), moduleId, moduleLabel, device.getCreated(), device.getUpdated(), device.getLocation());
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
		return new Location(node.path(1).decimalValue(), node.path(0).decimalValue());
	}
}
