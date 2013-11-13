package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaSleepTask extends Task {

	public static final String TYPE = "bodymedia-sleep";

	public BodyMediaSleepTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaSleepTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public BodyMediaSleepTask copy() {
		return copy(getClass());
	}
}
