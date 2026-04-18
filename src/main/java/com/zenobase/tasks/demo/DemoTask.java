package com.zenobase.tasks.demo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import java.util.Objects;

public class DemoTask extends Task {

	public static final String TYPE = "demo";
	public static final TokenField TAG = new TokenField("tag");

	public DemoTask(ObjectNode node) {
		super(node);
	}

	public DemoTask(String bucketId, Identity principal, String tag) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
	}

	public String getTag() {
		return Objects.requireNonNull(getSetting(TAG));
	}

	@Override
	public DemoTask copy() {
		return copy(getClass());
	}
}
