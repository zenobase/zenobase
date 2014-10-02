package com.zenobase.tasks.jawbone;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class JawboneStepsTask extends Task {

	public static final String TYPE = "jawbone-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField HOURLY = new BooleanField("hourly");
	public static final BooleanField METRIC = new BooleanField("metric");

	public JawboneStepsTask(ObjectNode node) {
		super(node);
	}

	public JawboneStepsTask(String bucketId, Identity principal, String tag, boolean hourly, boolean metric) {
		this(bucketId, principal, tag, hourly, metric, null);
	}

	JawboneStepsTask(String bucketId, Identity principal, String tag, boolean hourly, boolean metric, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(HOURLY, hourly);
		setSetting(METRIC, metric);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public boolean isHourly() {
		return getSetting(HOURLY);
	}

	public boolean isMetric() {
		return getSetting(METRIC);
	}

	@Override
	public JawboneStepsTask copy() {
		return copy(getClass());
	}
}
