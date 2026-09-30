package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class GoogleHealthSpo2Task extends GoogleHealthTaskSupport {

	public static final String TYPE = "google-health-spo2";
	public static final TokenField TAG = new TokenField("tag");

	public GoogleHealthSpo2Task(ObjectNode node) {
		super(node);
	}

	public GoogleHealthSpo2Task(String bucketId, Identity principal, DateTimeZone timezone, String tag, String marker) {
		super(TYPE, bucketId, principal, timezone, marker);
		setSetting(TAG, tag);
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	@Override
	public GoogleHealthSpo2Task copy() {
		return copy(getClass());
	}
}
