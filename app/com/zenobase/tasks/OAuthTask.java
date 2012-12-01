package com.zenobase.tasks;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.Field;
import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class OAuthTask extends Task {

	public static final OAuthTokenField TOKEN = new OAuthTokenField("token");

	public OAuthTask(ObjectNode node) {
		super(node);
	}

	public OAuthTask(String type, String bucketId, Identity principal) {
		super(type, bucketId, principal);
	}

	protected OAuthTask(String type, String bucketId, Identity principal, Token token) {
		super(type, bucketId, principal);
		setToken(token);
		setEnabled(true);
	}

	public Token getToken() {
		return getCredential(TOKEN);
	}

	public void setToken(Token token) {
		setCredential(TOKEN, token);
	}

	private static class OAuthTokenField extends Field<Token> {

		private static final TokenField VALUE = new TokenField("@value");
		private static final TokenField SECRET = new TokenField("secret");

		public OAuthTokenField(String name) {
			super(name, Token.class, "object");
		}

		@Override
		protected Token getValue(JsonNode node) {
			return new Token(VALUE.getValue((ObjectNode) node), SECRET.getValue((ObjectNode) node));
		}

		@Override
		public JsonNode toJson(Token value) {
			return value != null ?
				toJson(value.getToken(), value.getSecret()) :
				NullNode.getInstance();
		}

		private JsonNode toJson(String token, String secret) {
			ObjectNode node = Nodes.newObject();
			VALUE.setValue(node, token);
			SECRET.setValue(node, secret);
			return node;
		}
	}
}
