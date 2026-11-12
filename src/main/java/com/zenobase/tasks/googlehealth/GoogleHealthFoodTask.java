package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class GoogleHealthFoodTask extends GoogleHealthTaskSupport {

	public static final String TYPE = "google-health-food";
	public static final TokenField TAG = new TokenField("tag");

	public GoogleHealthFoodTask(ObjectNode node) {
		super(node);
	}

	public GoogleHealthFoodTask(String bucketId, Identity principal, DateTimeZone timezone, String tag, String marker) {
		super(TYPE, bucketId, principal, timezone, marker);
		setSetting(TAG, tag);
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	@Override
	public GoogleHealthFoodTask copy() {
		return copy(getClass());
	}
}
