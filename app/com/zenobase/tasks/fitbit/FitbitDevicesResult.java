package com.zenobase.tasks.fitbit;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

class FitbitDevicesResult {

	private final ArrayNode node;

	public FitbitDevicesResult(ArrayNode node) {
		this.node = node;
	}

	public LocalDate getLastDate() {
		for (JsonNode device : node) {
			if ("TRACKER".equals(device.path("type").textValue())) {
				return LocalDateTime.parse(device.path("lastSyncTime").textValue()).toLocalDate();
			}
		}
		Logger.warn("User does not have a Fitbit tracker:" + node);
		return null;
	}
}
