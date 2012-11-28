package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import com.google.common.base.Objects;

import com.zenobase.json.DateTimeField;
import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class DummyTask extends Task {

	public static final String TYPE = "dummy";

	private static final TokenField TAG = new TokenField("tag");
	private static final DateTimeField LATEST = new DateTimeField("latest");

	public DummyTask(ObjectNode node) {
		super(node);
	}

	public DummyTask(String bucketId, Identity principal, String tag) {
		super(TYPE, bucketId, principal);
		setTag(tag);
	}

	public String getTag() {
		return Objects.firstNonNull(getConfigValue(TAG), TYPE);
	}

	public void setTag(String tag) {
		setConfigValue(TAG, tag);
	}

	public DateTime getLatest() {
		return getConfigValue(LATEST);
	}

	public void setLatest(DateTime latest) {
		setConfigValue(LATEST, latest);
	}

	@Override
	public DummyTask copy() {
		return new DummyTask(Nodes.copy(toJson()));
	}
}
