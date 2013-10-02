package com.zenobase.tasks.bodymedia;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.models.Identity;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.tasks.OAuthTask;

public class BodyMediaTask extends OAuthTask {

	public static final String TYPE = "bodymedia";

	public BodyMediaTask(ObjectNode node) {
		super(node);
	}

	BodyMediaTask(String bucketId, Identity principal, Token token, String marker) {
		super(TYPE, bucketId, principal, token);
		setMarker(marker);
	}

	public boolean isExpired() {
		Token token = getToken();
		return token instanceof ExpiringToken &&
			((ExpiringToken) token).isExpired();
	}

	@Override
	public BodyMediaTask copy() {
		return copy(getClass());
	}
}
