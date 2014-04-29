package com.zenobase.tasks.mapmyfitness;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

class UserResult {

	private final JsonNode node;

	public UserResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public String getId() {
		String value = node.path("id").asText();
		Preconditions.checkArgument(!Strings.isNullOrEmpty(value), "Can't find user id: %s", node);
		return value;
	}

	public boolean isImperial() {
		String value = node.path("display_measurement_system").textValue();
		Preconditions.checkNotNull(value, "Can't find measurement system: %s", node);
		return "imperial".equals(value);
	}
}
