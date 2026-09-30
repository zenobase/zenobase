package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class GoogleHealthSleepTask extends GoogleHealthTaskSupport {

	public static final String TYPE = "google-health-sleep";
	public static final TokenField TAG = new TokenField("tag");

	public GoogleHealthSleepTask(ObjectNode node) {
		super(node);
	}

	public GoogleHealthSleepTask(
		String bucketId,
		Identity principal,
		DateTimeZone timezone,
		String tag,
		String marker
	) {
		super(TYPE, bucketId, principal, timezone, marker);
		setSetting(TAG, tag);
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	@Override
	public GoogleHealthSleepTask copy() {
		return copy(getClass());
	}
}
