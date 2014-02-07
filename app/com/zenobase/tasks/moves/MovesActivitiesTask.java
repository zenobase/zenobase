package com.zenobase.tasks.moves;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MovesActivitiesTask extends Task {

	public static final String TYPE = "moves-activities";

	public MovesActivitiesTask(ObjectNode node) {
		super(node);
	}

	public MovesActivitiesTask(String bucketId, Identity principal) {
		this(bucketId, principal, null);
	}

	MovesActivitiesTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public MovesActivitiesTask copy() {
		return copy(getClass());
	}
}
