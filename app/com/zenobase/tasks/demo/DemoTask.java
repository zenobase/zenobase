package com.zenobase.tasks.demo;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class DemoTask extends Task {

	public static final String TYPE = "demo";
	public static final TokenField TAG = new TokenField("tag");

	public DemoTask(ObjectNode node) {
		super(node);
	}

	public DemoTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public void setTag(String tag) {
		setSetting(TAG, tag);
	}

	@Override
	public DemoTask copy() {
		return copy(getClass());
	}
}
