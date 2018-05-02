package com.zenobase.tasks.dash;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTimeZone;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class DashTask extends Task {

	public static final String TYPE = "dash-trips";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public DashTask(ObjectNode node) {
		super(node);
	}

	public DashTask(String bucketId, Identity principal, String tag, DateTimeZone timezone) {
		this(bucketId, principal, tag, timezone, null);
	}

	DashTask(String bucketId, Identity principal, String tag, DateTimeZone timezone, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setTimezone(timezone);
		setMarker(marker);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public void setTimezone(DateTimeZone timezone) {
		setSetting(TIMEZONE, timezone != null ? timezone.getID() : null);
	}

	@Override
	public DashTask copy() {
		return copy(getClass());
	}
}
