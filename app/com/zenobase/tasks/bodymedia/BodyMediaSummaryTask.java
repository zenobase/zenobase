package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaSummaryTask extends Task {

	public static final String TYPE = "bodymedia";

	public BodyMediaSummaryTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaSummaryTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public BodyMediaSummaryTask copy() {
		return copy(getClass());
	}
}
