package com.zenobase.tasks.hexoskin;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;

import com.zenobase.models.Identity;

class HexoskinSleepResult extends HexoskinResultSupport {

	public HexoskinSleepResult(JsonNode node, Identity author, String tag, DateTimeZone zone, boolean metric) {
		super(node, author, tag, zone, metric);
	}
}
