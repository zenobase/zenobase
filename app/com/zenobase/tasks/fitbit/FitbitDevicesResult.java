package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

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
		return null;
	}
}
