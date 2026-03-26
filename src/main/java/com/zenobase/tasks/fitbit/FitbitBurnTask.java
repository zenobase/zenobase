package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class FitbitBurnTask extends Task {

	public static final String TYPE = "fitbit-burn";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField HOURLY = new BooleanField("hourly");

	public FitbitBurnTask(ObjectNode node) {
		super(node);
	}

	FitbitBurnTask(String bucketId, Identity principal, String marker, String tag, boolean hourly) {
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
	public FitbitBurnTask copy() {
		return copy(getClass());
	}
}
