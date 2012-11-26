package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;

public abstract class OAuthTask extends Task {

	private static final ObjectField TOKEN = new ObjectField("token");
	private static final TokenField VALUE = new TokenField("@value");
	private static final TokenField SECRET = new TokenField("secret");

	protected OAuthTask() {
		super(Generator.id());
	}

	protected OAuthTask(String id, Token token) {
		super(id);
		setToken(token);
	}

	public Token getToken() {
		ObjectNode node = getValue(TOKEN);
		return new Token(VALUE.getValue(node), SECRET.getValue(node));
	}

	public void setToken(Token token) {
		ObjectNode node = Nodes.newObject();
		VALUE.setValue(node, token.getToken());
		SECRET.setValue(node, token.getSecret());
		setValue(TOKEN, node);
	}
}
