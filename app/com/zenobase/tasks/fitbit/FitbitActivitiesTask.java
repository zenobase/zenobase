package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class FitbitActivitiesTask extends Task {

	public static final String TYPE = "fitbit-activities";

	public FitbitActivitiesTask(ObjectNode node) {
		super(node);
	}

	FitbitActivitiesTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public FitbitActivitiesTask copy() {
		return copy(getClass());
	}
}
