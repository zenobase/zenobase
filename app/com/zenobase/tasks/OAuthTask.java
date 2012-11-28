package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.Nodes;
import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class OAuthTask extends Task {

	private static final ObjectField TOKEN = new ObjectField("token");
	private static final TokenField VALUE = new TokenField("@value");
	private static final TokenField SECRET = new TokenField("secret");

	protected OAuthTask(ObjectNode node) {
		super(node);
	}

	protected OAuthTask(String type, String bucketId, Identity principal) {
		super(type, bucketId, principal);
	}

	protected OAuthTask(String id, String type, Task.State state, String bucketId, Identity principal, Token token) {
		super(id, type, state, bucketId, principal);
		setToken(token);
	}

	public Token getToken() {
		ObjectNode node = getConfigValue(TOKEN);
		return new Token(VALUE.getValue(node), SECRET.getValue(node));
	}

	public void setToken(Token token) {
		ObjectNode node = Nodes.newObject();
		VALUE.setValue(node, token.getToken());
		SECRET.setValue(node, token.getSecret());
		setConfigValue(TOKEN, node);
	}
}
