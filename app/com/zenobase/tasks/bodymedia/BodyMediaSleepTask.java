package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaSleepTask extends Task {

	public static final String TYPE = "bodymedia-sleep";
	public static final TokenField TAG = new TokenField("tag");

	public BodyMediaSleepTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaSleepTask(String bucketId, Identity principal, String marker, String tag) {
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
	public BodyMediaSleepTask copy() {
		return copy(getClass());
	}
}
