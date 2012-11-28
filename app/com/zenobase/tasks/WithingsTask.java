package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.IntegerField;
import com.zenobase.json.LongField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;

public class WithingsTask extends OAuthTask {

	public static final String TYPE = "withings";

	private static final IntegerField USER_ID = new IntegerField("userId");
	private static final LongField MARKER = new LongField("marker");

	public WithingsTask(ObjectNode node) {
		super(node);
	}

	public WithingsTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	public WithingsTask(String id, Task.State state, String bucketId, Identity principal, Token token, int userId, Long marker) {
		super(id, TYPE, state, bucketId, principal, token);
		setUserId(userId);
		setMarker(marker);
	}

	public int getUserId() {
		return getConfigValue(USER_ID);
	}

	public void setUserId(int userId) {
		setConfigValue(USER_ID, userId);
	}

	public Long getMarker() {
		return getConfigValue(MARKER);
	}

	public void setMarker(Long marker) {
		setConfigValue(MARKER, marker);
	}

	@Override
	public WithingsTask copy() {
		return new WithingsTask(Nodes.copy(toJson()));
	}
}
