package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class FitbitWeightTask extends Task {

	public static final String TYPE = "fitbit-weight";
	public static final TokenField TAG = new TokenField("tag");

	public FitbitWeightTask(ObjectNode node) {
		super(node);
	}

	FitbitWeightTask(String bucketId, Identity principal, String marker, String tag) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setTag(tag);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public void setTag(String tag) {
		setSetting(TAG, tag);
	}

	@Override
	public FitbitWeightTask copy() {
		return copy(getClass());
	}
}
