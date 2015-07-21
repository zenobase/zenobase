package com.zenobase.tasks.microsoft;

import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

public class MicrosoftHealthActivitiesTask extends Task {

	public static final String TYPE = "microsoft-activities";
	public static final BooleanField METRIC = new BooleanField("metric");

	public MicrosoftHealthActivitiesTask(ObjectNode node) {
		super(node);
	}

	public MicrosoftHealthActivitiesTask(String bucketId, Identity principal, boolean metric) {
		this(bucketId, principal, metric, null);
	}

	MicrosoftHealthActivitiesTask(String bucketId, Identity principal, boolean metric, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(METRIC, metric);
		setMarker(marker);
	}

	public boolean isMetric() {
		return Objects.firstNonNull(getSetting(METRIC), Boolean.TRUE);
	}

	@Override
	public MicrosoftHealthActivitiesTask copy() {
		return copy(getClass());
	}
}
