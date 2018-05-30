package com.zenobase.tasks.google;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;

public class GoogleFitLocateTask extends GoogleFitTaskSupport {

	public static final String TYPE = "google-locate";

	public GoogleFitLocateTask(ObjectNode node) {
		super(node);
	}

	public GoogleFitLocateTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal, null, null);
	}

	@Override
	public GoogleFitLocateTask copy() {
		return copy(getClass());
	}
}
