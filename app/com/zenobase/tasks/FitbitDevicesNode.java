package com.zenobase.tasks;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

class FitbitDevicesNode {

	private final ArrayNode node;

	public FitbitDevicesNode(ArrayNode node) {
		this.node = node;
	}

	public LocalDate getLastDate() {
		for (JsonNode device : node) {
			if ("TRACKER".equals(device.path("type").getTextValue())) {
				return LocalDateTime.parse(device.path("lastSyncTime").getTextValue()).toLocalDate().minusDays(1);
			}
		}
		return null;
	}
}
