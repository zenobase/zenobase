package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.IntegerField;
import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class WithingsTask extends OAuthTask {

	public static final String TYPE = "withings";

	private static final IntegerField USER_ID = new IntegerField("userId");
	private static final TokenField MARKER = new TokenField("marker");

	public WithingsTask(ObjectNode node) {
		super(node);
	}

	public WithingsTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	public WithingsTask(String id, Task.State state, String bucketId, Identity principal, Token token, int userId, String marker) {
		super(id, TYPE, state, bucketId, principal, token);
		setUserId(userId);
		setMarker(marker);
	}

	public int getUserId() {
		return getValue(USER_ID);
	}

	public void setUserId(int userId) {
		setValue(USER_ID, userId);
	}

	public String getMarker() {
		return getValue(MARKER);
	}

	public void setMarker(String marker) {
		setValue(MARKER, marker);
	}

	@Override
	public WithingsTask copy() {
		return new WithingsTask(Nodes.copy(toJson()));
	}
}
