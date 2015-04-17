package com.zenobase.tasks.trakt;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

class TraktSettingsResult {

	private final JsonNode node;

	public TraktSettingsResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public String getUsername() {
		return Preconditions.checkNotNull(node.path("user").path("username").textValue());
	}

	public DateTimeZone getTimeZone() {
		return DateTimeZone.forID(node.path("account").path("timezone").textValue());
	}
}
