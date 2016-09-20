package com.zenobase.tasks.automatic;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

class VehicleResult {

	private final JsonNode node;

	public VehicleResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public String getDisplayName() {
		String displayName = node.path("display_name").textValue();
	    if (displayName == null) {
	        String year = node.path("year").textValue();
            String make = node.path("make").textValue();
            String model = node.path("model").textValue();
            displayName = Strings.emptyToNull(Joiner.on(" ").skipNulls().join(year, make, model));
	    }
		return displayName;
	}
}
