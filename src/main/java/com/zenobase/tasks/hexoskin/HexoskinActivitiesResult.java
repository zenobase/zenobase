package com.zenobase.tasks.hexoskin;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;

class HexoskinActivitiesResult extends HexoskinResultSupport {

	public HexoskinActivitiesResult(JsonNode node, Identity author, String tag, DateTimeZone zone, boolean metric) {
		super(node, author, tag, zone, metric);
	}

	@Override
	protected boolean ignore(JsonNode node) {
		return super.ignore(node)
				|| "/api/v1/trainingroutine/12/"
						.equals(node.path("trainingroutine").textValue()); // sleep
	}
}
