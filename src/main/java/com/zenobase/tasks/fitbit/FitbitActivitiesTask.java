package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;

import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class FitbitActivitiesTask extends Task {

	public static final String TYPE = "fitbit-activities";
	public static final BooleanField AUTODETECTED = new BooleanField("autodetected");

	public FitbitActivitiesTask(ObjectNode node) {
		super(node);
	}

	FitbitActivitiesTask(String bucketId, Identity principal, String marker, boolean autodetected) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(AUTODETECTED, autodetected);
	}

	public boolean includeAutodetected() {
		return MoreObjects.firstNonNull(getSetting(AUTODETECTED), false);
	}

	@Override
	public FitbitActivitiesTask copy() {
		return copy(getClass());
	}
}
