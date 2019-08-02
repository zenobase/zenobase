package com.zenobase.tasks.oura;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class OuraStepsTask extends Task {

	public static final String TYPE = "oura-steps";
	public static final TokenField TAG = new TokenField("tag");

	public OuraStepsTask(ObjectNode node) {
		super(node);
	}

	public OuraStepsTask(String bucketId, Identity principal, String marker, String tag) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
	}

	public DateTime getBegin() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public String getTag() {
		return getSetting(TAG);
	}

	@Override
	public OuraStepsTask copy() {
		return copy(getClass());
	}
}
