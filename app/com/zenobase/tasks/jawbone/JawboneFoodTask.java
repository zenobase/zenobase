package com.zenobase.tasks.jawbone;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class JawboneFoodTask extends Task {

	public static final String TYPE = "jawbone-food";
	public static final TokenField TAG = new TokenField("tag");

	public JawboneFoodTask(ObjectNode node) {
		super(node);
	}

	public JawboneFoodTask(String bucketId, Identity principal, String tag) {
		this(bucketId, principal, tag, null);
	}

	JawboneFoodTask(String bucketId, Identity principal, String tag, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	@Override
	public JawboneFoodTask copy() {
		return copy(getClass());
	}
}
