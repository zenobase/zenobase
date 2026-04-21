package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.jspecify.annotations.Nullable;

/**
 * Common base for Google Health tasks. Mirrors {@link com.zenobase.tasks.google.GoogleFitTaskSupport}: the marker is an
 * ISO-8601 {@link DateTime} (Google Health uses RFC3339 timestamps throughout), and a timezone setting controls how the
 * API's {@code startTime}/{@code endTime} ranges are bucketed.
 */
abstract class GoogleHealthTaskSupport extends Task {

	public static final TokenField TIMEZONE = new TokenField("timezone");

	protected GoogleHealthTaskSupport(ObjectNode node) {
		super(node);
	}

	protected GoogleHealthTaskSupport(
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
