package com.zenobase.tasks.bodymedia;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

class BodyMediaTimezonesResult {

	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZZ").withOffsetParsed();

	private final ObjectNode node;

	public BodyMediaTimezonesResult(ObjectNode node) {
		this.node = node;
	}

	public TimezoneMap getTimezoneMap() {
		TimezoneMap timezones = new TimezoneMap();
		for (JsonNode tzNode : node.path("timezones")) {
			DateTimeZone tz = getDateTimeZone(tzNode.path("value"));
			DateTime from = getDateTime(tzNode.path("startDate"));
			DateTime to = getDateTime(tzNode.path("endDate"));
			timezones.add(from, to, tz);
		}
		return timezones;
	}

	private static DateTimeZone getDateTimeZone(JsonNode node) {
		Preconditions.checkState(!node.isMissingNode(), "missing timezone");
		return DateTimeZone.forID(node.textValue());
	}

	private static DateTime getDateTime(JsonNode node) {
		return !node.isMissingNode() ? DateTime.parse(node.textValue(), DATE_TIME_FORMAT) : null;
	}
}
