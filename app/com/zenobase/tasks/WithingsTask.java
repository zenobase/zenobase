package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.IntegerField;
import com.zenobase.models.Identity;

public class WithingsTask extends OAuthTask {

	public static final String TYPE = "withings";
	public static final IntegerField USER_ID = new IntegerField("userId");

	public WithingsTask(ObjectNode node) {
		super(node);
	}

	WithingsTask(String bucketId, Identity principal, Token token, Integer userId, String marker) {
		super(TYPE, bucketId, principal, token);
		setCredential(USER_ID, userId);
		setMarker(marker);
	}

	public Integer getUserId() {
		return getCredential(USER_ID);
	}

	@Override
	public WithingsTask copy() {
		return copy(getClass());
	}
}
