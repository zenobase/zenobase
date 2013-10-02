package com.zenobase.tasks.netatmo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthTask;

public class NetatmoTask extends OAuthTask {

	public static final String TYPE = "netatmo";

	public NetatmoTask(ObjectNode node) {
		super(node);
	}

	public NetatmoTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
		setCredential(TOKEN, Token.empty());
	}

	NetatmoTask(String bucketId, Identity principal, Token accessToken, String marker) {
		super(TYPE, bucketId, principal, accessToken);
		setMarker(marker);
	}

	@Override
	public NetatmoTask copy() {
		return copy(getClass());
	}
}
