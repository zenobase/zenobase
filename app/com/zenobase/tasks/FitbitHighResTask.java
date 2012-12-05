package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.models.Identity;

public class FitbitHighResTask extends OAuthTask {

	public static final String TYPE = "fitbit-highres";

	public FitbitHighResTask(ObjectNode node) {
		super(node);
	}

	FitbitHighResTask(String bucketId, Identity principal, Token accessToken, String marker) {
		super(TYPE, bucketId, principal, accessToken);
		setMarker(marker);
	}

	@Override
	public FitbitHighResTask copy() {
		return copy(getClass());
	}
}
