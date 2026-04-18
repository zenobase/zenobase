package com.zenobase.tasks.withings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class WithingsSleepTask extends Task {

	public static final String TYPE = "withings-sleep";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField TIMEZONE = new TokenField("timezone");
	public static final BooleanField RANGES = new BooleanField("ranges");

	public WithingsSleepTask(ObjectNode node) {
		super(node);
	}

	WithingsSleepTask(String bucketId, Identity principal, @Nullable String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(RANGES, true);
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	public void setTag(String tag) {
		setSetting(TAG, tag);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public void setTimezone(DateTimeZone timezone) {
		setSetting(TIMEZONE, timezone.getID());
	}

	public @Nullable DateTime getFrom() {
		String value = getMarker();
		try {
			return value != null ? DateTime.parse(value) : null;
		} catch (IllegalArgumentException e) {
			// legacy
			return new DateTime(Long.parseLong(value) * 1000, getTimezone());
		}
	}

	public boolean useRanges() {
		return MoreObjects.firstNonNull(getSetting(RANGES), false);
	}

	@Override
	public WithingsSleepTask copy() {
		return copy(getClass());
	}
}
