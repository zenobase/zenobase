package com.zenobase.tasks.google;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.jspecify.annotations.Nullable;

abstract class GoogleFitTaskSupport extends Task {

	public static final TokenField TIMEZONE = new TokenField("timezone");

	public GoogleFitTaskSupport(ObjectNode node) {
		super(node);
	}

	public GoogleFitTaskSupport(
		String type,
		String bucketId,
		Identity principal,
		@Nullable DateTimeZone timezone,
		String marker
	) {
		super(type, bucketId, principal);
		if (timezone != null) {
			setSetting(TIMEZONE, timezone.getID());
		}
		setMarker(marker);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public @Nullable DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker, ISODateTimeFormat.dateTime().withOffsetParsed()) : null;
	}
}
