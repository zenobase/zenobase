package com.zenobase.tasks.garmin;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class GarminActivitiesTask extends Task {

	public static final String TYPE = "garmin-activities";

	public GarminActivitiesTask(ObjectNode node) {
		super(node);
	}

	public GarminActivitiesTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public GarminActivitiesTask copy() {
		return copy(getClass());
	}
}
