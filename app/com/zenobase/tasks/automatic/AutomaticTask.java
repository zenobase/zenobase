package com.zenobase.tasks.automatic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class AutomaticTask extends Task {

	public static final String TYPE = "automatic-trips";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField METRIC = new BooleanField("metric");

	public AutomaticTask(ObjectNode node) {
		super(node);
	}

	public AutomaticTask(String bucketId, Identity principal, String tag, boolean metric) {
		this(bucketId, principal, tag, metric, null);
	}

	AutomaticTask(String bucketId, Identity principal, String tag, boolean metric, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setSetting(METRIC, metric);
		setMarker(marker);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public boolean isMetric() {
		return Objects.firstNonNull(getSetting(METRIC), Boolean.TRUE);
	}

	@Override
	public AutomaticTask copy() {
		return copy(getClass());
	}
}
