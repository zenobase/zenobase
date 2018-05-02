package com.zenobase.tasks.jawbone;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class JawboneActivitiesTask extends Task {

	public static final String TYPE = "jawbone-activities";
	public static final BooleanField METRIC = new BooleanField("metric");

	public JawboneActivitiesTask(ObjectNode node) {
		super(node);
	}

	public JawboneActivitiesTask(String bucketId, Identity principal, boolean metric) {
		this(bucketId, principal, metric, null);
	}

	JawboneActivitiesTask(String bucketId, Identity principal, boolean metric, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(METRIC, metric);
	}

	public boolean isMetric() {
		return getSetting(METRIC);
	}

	@Override
	public JawboneActivitiesTask copy() {
		return copy(getClass());
	}
}
