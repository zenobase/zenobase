package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.models.Identity;

public class FitbitTask extends OAuthTask {

	public static final String TYPE = "fitbit";

	public FitbitTask(ObjectNode node) {
		super(node);
	}

	FitbitTask(String bucketId, Identity principal, Token accessToken, String marker) {
		super(TYPE, bucketId, principal, accessToken);
		setMarker(marker);
	}

	@Override
	public FitbitTask copy() {
		return copy(getClass());
	}
}
