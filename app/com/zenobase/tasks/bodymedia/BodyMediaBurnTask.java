package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaBurnTask extends Task {

	public static final String TYPE = "bodymedia-burn-hour";

	public BodyMediaBurnTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaBurnTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public BodyMediaBurnTask copy() {
		return copy(getClass());
	}
}
