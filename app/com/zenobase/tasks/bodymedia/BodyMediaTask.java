package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaTask extends Task {

	public static final String TYPE = "bodymedia";

	public BodyMediaTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public BodyMediaTask copy() {
		return copy(getClass());
	}
}
