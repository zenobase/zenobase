package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaBurnTask extends Task {

	public static final String TYPE = "bodymedia-burn";
	public static final TokenField TAG = new TokenField("tag");

	public BodyMediaBurnTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaBurnTask(String bucketId, Identity principal, String marker, String tag) {
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
	public BodyMediaBurnTask copy() {
		return copy(getClass());
	}
}
