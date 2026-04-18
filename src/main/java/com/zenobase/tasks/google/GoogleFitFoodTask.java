package com.zenobase.tasks.google;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class GoogleFitFoodTask extends GoogleFitTaskSupport {

	public static final String TYPE = "google-food";
	public static final TokenField TAG = new TokenField("tag");

	public GoogleFitFoodTask(ObjectNode node) {
		super(node);
	}

	public GoogleFitFoodTask(String bucketId, Identity principal, DateTimeZone timezone, String tag, String marker) {
		super(TYPE, bucketId, principal, timezone, marker);
		setSetting(TAG, tag);
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	@Override
	public GoogleFitFoodTask copy() {
		return copy(getClass());
	}
}
