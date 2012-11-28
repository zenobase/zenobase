package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;

public class TwitterTask extends OAuthTask {

	public static final String TYPE = "twitter";

	public TwitterTask(ObjectNode node) {
		super(node);
	}

	public TwitterTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	public TwitterTask(String id, String bucketId, Identity principal, Token accessToken) {
		super(id, TYPE, bucketId, principal, accessToken);
	}

	@Override
	public TwitterTask copy() {
		return new TwitterTask(Nodes.copy(toJson()));
	}
}
