package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.IntegerField;
import com.zenobase.json.LongField;
import com.zenobase.models.Identity;

public class WithingsTask extends OAuthTask {

	public static final String TYPE = "withings";

	public static final IntegerField USER_ID = new IntegerField("userId");
	public static final LongField MARKER = new LongField("marker");

	public WithingsTask(ObjectNode node) {
		super(node);
	}

	public WithingsTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	public WithingsTask(String id, Task.State state, String bucketId, Identity principal, Token token, Integer userId, Long marker) {
		super(id, TYPE, state, bucketId, principal, token);
		setConfigValue(USER_ID, userId);
		setConfigValue(MARKER, marker);
	}

	public Integer getUserId() {
		return getConfigValue(USER_ID);
	}

	public Long getMarker() {
		return getConfigValue(MARKER);
	}

	@Override
	public WithingsTask copy() {
		return copy(getClass());
	}
}
