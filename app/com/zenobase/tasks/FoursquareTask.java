package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.models.Identity;

public class FoursquareTask extends OAuthTask {

	public static final String TYPE = "foursquare";

	public FoursquareTask(ObjectNode node) {
		super(node);
	}

	public FoursquareTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
		setCredential(TOKEN, Token.empty());
	}

	FoursquareTask(String bucketId, Identity principal, Token accessToken, String marker) {
		super(TYPE, bucketId, principal, accessToken);
		setMarker(marker);
	}

	@Override
	public FoursquareTask copy() {
		return copy(getClass());
	}
}
