package com.zenobase.tasks.google;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

abstract class GoogleFitTaskSupport extends Task {

	public static final TokenField TIMEZONE = new TokenField("timezone");

	public GoogleFitTaskSupport(ObjectNode node) {
		super(node);
	}

	public GoogleFitTaskSupport(String type, String bucketId, Identity principal, DateTimeZone timezone, String marker) {
		super(type, bucketId, principal);
		setSetting(TIMEZONE, timezone != null ? timezone.getID() : null);
		setMarker(marker);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker, ISODateTimeFormat.dateTime().withOffsetParsed()) : null;
	}
}
