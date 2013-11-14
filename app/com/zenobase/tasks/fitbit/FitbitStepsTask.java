package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class FitbitStepsTask extends Task {

	public static final String TYPE = "fitbit-steps";
	public static final TokenField TAG = new TokenField("tag");

	public FitbitStepsTask(ObjectNode node) {
		super(node);
	}

	FitbitStepsTask(String bucketId, Identity principal, String marker, String tag) {
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
	public FitbitStepsTask copy() {
		return copy(getClass());
	}
}
