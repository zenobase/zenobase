package com.zenobase.tasks.moves;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

class ProfileResult {

	private final JsonNode node;

	public ProfileResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public DateTime getFirstDate() {
		String zoneId = node.path("profile").path("currentTimeZone").path("id").textValue();
		String firstDate = node.path("profile").path("firstDate").textValue();
		DateTimeZone zone = DateTimeZone.forID(zoneId);
		LocalDate date = LocalDate.parse(firstDate, ISODateTimeFormat.basicDate());
		return date.toDateTimeAtStartOfDay(zone);
	}
}
