package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class GoogleHealthRespiratoryTask extends GoogleHealthTaskSupport {

	public static final String TYPE = "google-health-respiratory";
	public static final TokenField TAG = new TokenField("tag");

	public GoogleHealthRespiratoryTask(ObjectNode node) {
		super(node);
	}

	public GoogleHealthRespiratoryTask(
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
	public GoogleHealthRespiratoryTask copy() {
		return copy(getClass());
	}
}
