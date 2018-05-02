package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class FitbitSleepTask extends Task {

	public static final String TYPE = "fitbit-sleep";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField RANGES = new BooleanField("ranges");

	public FitbitSleepTask(ObjectNode node) {
		super(node);
	}

	FitbitSleepTask(String bucketId, Identity principal, String marker, String tag) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setTag(tag);
		setSetting(RANGES, true);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public void setTag(String tag) {
		setSetting(TAG, tag);
	}

	public boolean useRanges() {
		return Objects.firstNonNull(getSetting(RANGES), false);
	}

	@Override
	public FitbitSleepTask copy() {
		return copy(getClass());
	}
}
