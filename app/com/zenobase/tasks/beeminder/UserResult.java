package com.zenobase.tasks.beeminder;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class UserResult {

	private final ObjectNode node;

	public UserResult(ObjectNode node) {
		this.node = node;
	}

	public DateTimeZone getTimezone() {
		return DateTimeZone.forID(node.path("timezone").textValue());
	}

	public boolean hasGoal(String slug) {
		for (JsonNode goalNode : node.path("goals")) {
			if (slug.equals(goalNode.textValue())) {
				return true;
			}
		}
		return false;
	}
}
