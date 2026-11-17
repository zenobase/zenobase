package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class GoogleHealthCardioTask extends GoogleHealthTaskSupport {

	public static final String TYPE = "google-health-cardio";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField HOURLY = new BooleanField("hourly");

	public GoogleHealthCardioTask(ObjectNode node) {
		super(node);
	}

	public GoogleHealthCardioTask(
		String bucketId,
		Identity principal,
		DateTimeZone timezone,
		String tag,
		boolean hourly,
		String marker
	) {
		super(TYPE, bucketId, principal, timezone, marker);
		setSetting(TAG, tag);
		setSetting(HOURLY, hourly);
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	public boolean isHourly() {
		Boolean value = getSetting(HOURLY);
		return value != null && value;
	}

	@Override
	public GoogleHealthCardioTask copy() {
		return copy(getClass());
	}
}
