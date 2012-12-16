package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.models.Identity;

public class FitbitIntradayTask extends OAuthTask {

	public static final String TYPE = "fitbit-intraday";

	public FitbitIntradayTask(ObjectNode node) {
		super(node);
	}

	FitbitIntradayTask(String bucketId, Identity principal, Token accessToken, String marker) {
		super(TYPE, bucketId, principal, accessToken);
		setMarker(marker);
	}

	@Override
	public FitbitIntradayTask copy() {
		return copy(getClass());
	}
}
