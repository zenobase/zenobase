package com.zenobase.tasks.hexoskin;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.models.Identity;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

class HexoskinSleepResult extends HexoskinResultSupport {

	public HexoskinSleepResult(
		JsonNode node,
		Identity author,
		@Nullable String tag,
		DateTimeZone zone,
		boolean metric
	) {
		super(node, author, tag, zone, metric);
	}
}
