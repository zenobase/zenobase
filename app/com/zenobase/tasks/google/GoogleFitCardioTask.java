package com.zenobase.tasks.google;

import org.joda.time.DateTimeZone;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class GoogleFitCardioTask extends GoogleFitTaskSupport {

	public static final String TYPE = "google-cardio";
	public static final TokenField TAG = new TokenField("tag");

	public GoogleFitCardioTask(String bucketId, Identity principal, DateTimeZone timezone, String tag, String marker) {
		super(TYPE, bucketId, principal, timezone, marker);
		setSetting(TAG, tag);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	@Override
	public GoogleFitCardioTask copy() {
		return copy(getClass());
	}
}
