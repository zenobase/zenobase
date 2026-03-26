package com.zenobase.tasks.strava;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class StravaTask extends Task {

	public static final String TYPE = "strava-activities";
	public static final BooleanField METRIC = new BooleanField("metric");

	public StravaTask(ObjectNode node) {
		super(node);
	}

	public StravaTask(String bucketId, Identity principal, boolean metric) {
		this(bucketId, principal, metric, null);
	}

	StravaTask(String bucketId, Identity principal, boolean metric, @Nullable String marker) {
		super(TYPE, bucketId, principal);
		setSetting(METRIC, metric);
		setMarker(marker);
	}

	public boolean isMetric() {
		return MoreObjects.firstNonNull(getSetting(METRIC), Boolean.TRUE);
	}

	@Override
	public StravaTask copy() {
		return copy(getClass());
	}
}
