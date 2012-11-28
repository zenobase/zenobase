package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class DummyTask extends Task {

	public static final String TYPE = "dummy";
	public static final TokenField TAG = new TokenField("tag");

	public DummyTask(ObjectNode node) {
		super(node);
	}

	public DummyTask(String bucketId, Identity principal, String tag) {
		super(TYPE, bucketId, principal);
		setTag(tag);
	}

	public String getTag() {
		return getConfigValue(TAG);
	}

	public void setTag(String tag) {
		setState(tag != null ? State.READY : State.SUSPENDED);
		setConfigValue(TAG, tag);
	}

	@Override
	public DummyTask copy() {
		return new DummyTask(Nodes.copy(toJson()));
	}
}
