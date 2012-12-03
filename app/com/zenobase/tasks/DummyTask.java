package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class DummyTask extends Task {

	public static final String TYPE = "dummy";
	public static final TokenField TAG = new TokenField("tag");

	public DummyTask(ObjectNode node) {
		super(node);
	}

	public DummyTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public void setTag(String tag) {
		setSetting(TAG, tag);
	}

	@Override
	public DummyTask copy() {
		return copy(getClass());
	}
}
