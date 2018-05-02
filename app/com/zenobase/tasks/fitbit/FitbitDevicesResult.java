package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.Logger;

class FitbitDevicesResult {

	private final ArrayNode node;

	public FitbitDevicesResult(ArrayNode node) {
		this.node = node;
	}

	/**
	 * @param deviceType TRACKER or SCALE
	 */
	public LocalDate getLastDate(DeviceType deviceType) {
		LocalDate latest = null;
		for (JsonNode device : node) {
			if (deviceType.name().equals(device.path("type").textValue())) {
				LocalDate date = LocalDateTime.parse(device.path("lastSyncTime").textValue()).toLocalDate();
				if (latest == null || date.isAfter(latest)) {
					latest = date;
				}
			}
		}
		if (latest == null) {
			Logger.warn("User does not have a device: {}", node);
		}
		return latest;
	}
}
