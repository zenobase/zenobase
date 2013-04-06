package com.zenobase.tasks;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.DateTimeField;
import com.zenobase.json.Field;
import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.oauth.RefreshableToken;

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
		private static final TokenField REFRESH = new TokenField("refresh");
		private static final DateTimeField EXPIRES = new DateTimeField("expires");

		public OAuthTokenField(String name) {
			super(name, Token.class, "object");
		}

		@Override
		protected Token getValue(JsonNode node) {
			return getToken((ObjectNode) node);
		}

		private Token getToken(ObjectNode node) {
			return new RefreshableToken(VALUE.getValue(node), SECRET.getValue(node), REFRESH.getValue(node), EXPIRES.getValue(node));
		}

		@Override
		public JsonNode toJson(Token value) {
			if (value == null) {
				return NullNode.getInstance();
			}
			ObjectNode node = Nodes.newObject();
			VALUE.setValue(node, value.getToken());
			SECRET.setValue(node, value.getSecret());
			if (value instanceof RefreshableToken) {
				REFRESH.setValue(node, ((RefreshableToken) value).getRefreshToken());
				EXPIRES.setValue(node, ((RefreshableToken) value).getExpires());
			}
			return node;
		}
	}
}
