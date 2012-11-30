package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.models.Identity;

public class TwitterTask extends OAuthTask {

	public static final String TYPE = "twitter";

	public TwitterTask(ObjectNode node) {
		super(node);
	}

	TwitterTask(String bucketId, Identity principal, Token accessToken) {
		super(TYPE, bucketId, principal, accessToken);
	}

	@Override
	public TwitterTask copy() {
		return copy(getClass());
	}
}
