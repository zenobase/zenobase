package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaStepsTask extends Task {

	public static final String TYPE = "bodymedia-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField HOURLY = new BooleanField("hourly");

	public BodyMediaStepsTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaStepsTask(String bucketId, Identity principal, String tag, boolean hourly, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(HOURLY, hourly);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public boolean isHourly() {
		return Objects.firstNonNull(getSetting(HOURLY), true);
	}

	@Override
	public BodyMediaStepsTask copy() {
		return copy(getClass());
	}
}
