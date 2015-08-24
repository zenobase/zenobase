package com.zenobase.tasks.netatmo;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Location;

class DevicesResult {

	private final JsonNode node;
	private final boolean includeModules;

	public DevicesResult(JsonNode node, boolean includeModules) {
		this.node = Preconditions.checkNotNull(node);
		this.includeModules = includeModules;
	}

	public boolean isSuccess() {
		return "ok".equals(node.path("status").textValue());
	}

	public Collection<Device> getDevices() {
		Preconditions.checkState(isSuccess(), "Expected a successful response but got <%s>", node);
		Map<String, Device> devices = Maps.newLinkedHashMap();
		for (JsonNode item : node.path("body").path("devices")) {
			addDevice(item, devices);
		}
		if (includeModules) {
			for (JsonNode item : node.path("body").path("modules")) {
				addModule(item, devices);
			}
		}
		return devices.values();
	}

	private static void addDevice(JsonNode node, Map<String, Device> devices) {
		String id = node.path("_id").textValue();
		String label = node.path("module_name").textValue();
		DateTime created = getTimestamp(node.path("date_setup").path("sec"), DateTimeZone.UTC);
		DateTime updated = getTimestamp(node.path("last_status_store"), getTimezone(node.path("place").path("timezone")));
		Location location = getLocation(node.path("place").path("location"));
		devices.put(id, new Device(id, label, created, updated, location));
	}

	private static void addModule(JsonNode node, Map<String, Device> devices) {
		String moduleId = node.path("_id").textValue();
		String moduleLabel = node.path("module_name").textValue();
		String parentId = node.path("main_device").textValue();
		Device parent = devices.get(parentId);
		Preconditions.checkNotNull(parent, "Can't find module %s in %s", parentId, devices.keySet());
		devices.put(moduleId, new Device(parentId, moduleId, moduleLabel, parent.getCreated(), parent.getUpdated(), parent.getLocation()));
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
