package com.zenobase.tasks.google;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class GoogleFitCardioTask extends GoogleFitTaskSupport {

	public static final String TYPE = "google-cardio";
	public static final TokenField TAG = new TokenField("tag");

	public GoogleFitCardioTask(ObjectNode node) {
		super(node);
	}

	public GoogleFitCardioTask(String bucketId, Identity principal, DateTimeZone timezone, String tag, String marker) {
		super(TYPE, bucketId, principal, timezone, marker);
		setSetting(TAG, tag);
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	@Override
	public GoogleFitCardioTask copy() {
		return copy(getClass());
	}
}
