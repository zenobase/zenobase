package com.zenobase.tasks.misfit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MisfitStepsTask extends Task {

	public static final String TYPE = "misfit-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public MisfitStepsTask(ObjectNode node) {
		super(node);
	}

	public MisfitStepsTask(String bucketId, Identity principal, String tag, DateTimeZone zone, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setSetting(TIMEZONE, zone.getID());
		setMarker(marker);
	}

	public DateTime getBegin() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	@Override
	public MisfitStepsTask copy() {
		return copy(getClass());
	}
}
