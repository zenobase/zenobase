package com.zenobase.tasks.trakt;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;

class TraktSettingsResult {

	private final JsonNode node;

	public TraktSettingsResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public DateTimeZone getTimeZone() {
		return DateTimeZone.forID(node.path("account").path("timezone").textValue());
	}
}
