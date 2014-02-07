package com.zenobase.tasks.moves;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MovesPlacesTask extends Task {

	public static final String TYPE = "moves-places";

	public MovesPlacesTask(ObjectNode node) {
		super(node);
	}

	public MovesPlacesTask(String bucketId, Identity principal) {
		this(bucketId, principal, null);
	}

	MovesPlacesTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public MovesPlacesTask copy() {
		return copy(getClass());
	}
}
