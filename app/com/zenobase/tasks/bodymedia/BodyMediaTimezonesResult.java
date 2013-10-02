package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

class BodyMediaTimezonesResult {

	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZZ").withOffsetParsed();

	private final ObjectNode node;

	public BodyMediaTimezonesResult(ObjectNode node) {
		this.node = node;
	}

	public RangeMap<LocalDateTime, DateTimeZone> getTimezones() {
		RangeMap<LocalDateTime, DateTimeZone> timezones = TreeRangeMap.create();
		for (JsonNode tzNode : node.path("timezones")) {
			DateTimeZone tz = getDateTimeZone(tzNode.path("value"));
			LocalDateTime from = getLocalDateTime(tzNode.path("startDate"));
			LocalDateTime to = getLocalDateTime(tzNode.path("endDate"));
			timezones.put(getRange(from, to), tz);
		}
		return timezones;
	}

	private static DateTimeZone getDateTimeZone(JsonNode node) {
		Preconditions.checkState(!node.isMissingNode(), "missing timezone");
		return DateTimeZone.forID(node.textValue());
	}

	private static LocalDateTime getLocalDateTime(JsonNode node) {
		return !node.isMissingNode() ? DateTime.parse(node.textValue(), DATE_TIME_FORMAT).toLocalDateTime() : null;
	}

	private static Range<LocalDateTime> getRange(LocalDateTime from, LocalDateTime to) {
		Preconditions.checkNotNull(from, "missing start time");
		return to != null ? Range.closedOpen(from, to) : Range.atLeast(from);
	}
}
