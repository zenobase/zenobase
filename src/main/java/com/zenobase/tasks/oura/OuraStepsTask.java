package com.zenobase.tasks.oura;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class OuraStepsTask extends Task {

	public static final String TYPE = "oura-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public OuraStepsTask(ObjectNode node) {
		super(node);
	}

	public OuraStepsTask(String bucketId, Identity principal, String marker, String tag, @Nullable DateTimeZone zone) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		if (zone != null) {
			setSetting(TIMEZONE, zone.getID());
		}
	}

	public @Nullable DateTime getBegin() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	@Override
	public OuraStepsTask copy() {
		return copy(getClass());
	}
}
