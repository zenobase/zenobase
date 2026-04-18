package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import org.jspecify.annotations.Nullable;

public class FitbitCardioTask extends Task {

	public static final String TYPE = "fitbit-cardio";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField HOURLY = new BooleanField("hourly");

	public FitbitCardioTask(ObjectNode node) {
		super(node);
	}

	FitbitCardioTask(String bucketId, Identity principal, String marker, String tag, boolean hourly) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(HOURLY, hourly);
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	public boolean isHourly() {
		return MoreObjects.firstNonNull(getSetting(HOURLY), false);
	}

	@Override
	public FitbitCardioTask copy() {
		return copy(getClass());
	}
}
