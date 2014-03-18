package com.zenobase.tasks.withings;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class WithingsSleepTask extends Task {

	public static final String TYPE = "withings-sleep";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public WithingsSleepTask(ObjectNode node) {
		super(node);
	}

	WithingsSleepTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	public String getTag() {
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
		setSetting(TIMEZONE, timezone != null ? timezone.getID() : null);
	}

	public DateTime getFrom() {
		String value = getMarker();
		return value != null ? new DateTime(Long.parseLong(value) * 1000, getTimezone()) : null;
	}

	@Override
	public WithingsSleepTask copy() {
		return copy(getClass());
	}
}
