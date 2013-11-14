package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaStepsTask extends Task {

	public static final String TYPE = "bodymedia-steps";

	public BodyMediaStepsTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaStepsTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public BodyMediaStepsTask copy() {
		return copy(getClass());
	}
}
