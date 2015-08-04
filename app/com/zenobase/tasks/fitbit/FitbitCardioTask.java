package com.zenobase.tasks.fitbit;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

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

	public String getTag() {
		return getSetting(TAG);
	}

	public boolean isHourly() {
		return Objects.firstNonNull(getSetting(HOURLY), false);
	}

	@Override
	public FitbitCardioTask copy() {
		return copy(getClass());
	}
}
