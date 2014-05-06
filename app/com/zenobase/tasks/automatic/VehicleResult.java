package com.zenobase.tasks.automatic;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

class VehicleResult {

	private final JsonNode node;

	public VehicleResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public String getDisplayName() {
		return node.path("display_name").textValue();
	}
}
