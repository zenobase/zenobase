package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.models.Identity;

public class BodyMediaTask extends OAuthTask {

	public static final String TYPE = "bodymedia";

	public BodyMediaTask(ObjectNode node) {
		super(node);
	}

	BodyMediaTask(String bucketId, Identity principal, Token token, String marker) {
		super(TYPE, bucketId, principal, token);
		setMarker(marker);
	}

	@Override
	public BodyMediaTask copy() {
		return copy(getClass());
	}
}
