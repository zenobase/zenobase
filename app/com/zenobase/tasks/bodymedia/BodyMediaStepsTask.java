package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaStepsTask extends Task {

	public static final String TYPE = "bodymedia-steps";
	public static final TokenField TAG = new TokenField("tag");

	public BodyMediaStepsTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaStepsTask(String bucketId, Identity principal, String marker, String tag) {
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
	public BodyMediaStepsTask copy() {
		return copy(getClass());
	}
}
