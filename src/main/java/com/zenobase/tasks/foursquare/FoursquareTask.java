package com.zenobase.tasks.foursquare;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class FoursquareTask extends Task {

	public static final String TYPE = "foursquare";

	public FoursquareTask(ObjectNode node) {
		super(node);
	}

	public FoursquareTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	FoursquareTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public FoursquareTask copy() {
		return copy(getClass());
	}
}
