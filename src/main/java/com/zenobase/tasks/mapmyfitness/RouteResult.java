package com.zenobase.tasks.mapmyfitness;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Location;

class RouteResult {

	private final JsonNode node;

	public RouteResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public @Nullable Location getLocation() {
		JsonNode coordinatesNode = node.path("starting_location").path("coordinates");
		if (coordinatesNode.isMissingNode()) {
			return null;
		}
		BigDecimal lat = coordinatesNode.path(1).decimalValue();
		BigDecimal lon = coordinatesNode.path(0).decimalValue();
		return new Location(lat, lon);
	}
}
