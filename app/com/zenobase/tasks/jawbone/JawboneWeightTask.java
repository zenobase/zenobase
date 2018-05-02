package com.zenobase.tasks.jawbone;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class JawboneWeightTask extends Task {

	public static final String TYPE = "jawbone-weight";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField METRIC = new BooleanField("metric");

	public JawboneWeightTask(ObjectNode node) {
		super(node);
	}

	public JawboneWeightTask(String bucketId, Identity principal, String tag, boolean metric) {
		this(bucketId, principal, tag, metric, null);
	}

	JawboneWeightTask(String bucketId, Identity principal, String tag, boolean metric, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(METRIC, metric);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public boolean isMetric() {
		return getSetting(METRIC);
	}

	@Override
	public JawboneWeightTask copy() {
		return copy(getClass());
	}
}
