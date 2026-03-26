package com.zenobase.tasks.withings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class WithingsCardioTask extends Task {

	public static final String TYPE = "withings-cardio";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public WithingsCardioTask(ObjectNode node) {
		super(node);
	}

	WithingsCardioTask(String bucketId, Identity principal, @Nullable String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
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

	@Override
	public WithingsCardioTask copy() {
		return copy(getClass());
	}
}
