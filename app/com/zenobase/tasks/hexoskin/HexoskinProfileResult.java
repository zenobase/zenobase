package com.zenobase.tasks.hexoskin;

import com.fasterxml.jackson.databind.JsonNode;

class HexoskinProfileResult {

	private final JsonNode node;

	public HexoskinProfileResult(JsonNode node) {
		this.node = node;
	}

	public boolean isMetric() {
		return !"imperial".equals(node.path("objects").path(0).path("profile").path("unit_system").textValue());
	}
}
