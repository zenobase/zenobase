package com.zenobase.tasks.bodymedia;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.models.Identity;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.tasks.OAuthTask;

public class BodyMediaBurnTask extends OAuthTask {

	public static final String TYPE = "bodymedia-burn";

	public BodyMediaBurnTask(ObjectNode node) {
		super(node);
	}

	BodyMediaBurnTask(String bucketId, Identity principal, Token token, String marker) {
		super(TYPE, bucketId, principal, token);
		setMarker(marker);
	}

	public boolean isExpired() {
		Token token = getToken();
		return token instanceof ExpiringToken &&
			((ExpiringToken) token).isExpired();
	}

	@Override
	public BodyMediaBurnTask copy() {
		return copy(getClass());
	}
}
