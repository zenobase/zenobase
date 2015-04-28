package com.zenobase.tasks.jawbone;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class JawboneMoodTask extends Task {

	public static final String TYPE = "jawbone-mood";
	public static final TokenField TAG = new TokenField("tag");

	public JawboneMoodTask(ObjectNode node) {
		super(node);
	}

	public JawboneMoodTask(String bucketId, Identity principal, String tag) {
		this(bucketId, principal, tag, null);
	}

	JawboneMoodTask(String bucketId, Identity principal, String tag, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	@Override
	public JawboneMoodTask copy() {
		return copy(getClass());
	}
}
