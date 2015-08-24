package com.zenobase.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.DateTimeField;
import com.zenobase.json.Field;
import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.oauth.ExpiringToken;

public class OAuthCredentials extends Credentials {

	public static final OAuthTokenField TOKEN = new OAuthTokenField("token");
	public static final TokenField SCOPE = new TokenField("scope", false);

	public OAuthCredentials(ObjectNode node) {
		super(node);
	}

	public OAuthCredentials(String type, Identity principal) {
		super(type, principal);
	}

	protected OAuthCredentials(String type, Identity principal, Token token) {
		super(type, principal);
		setToken(token);
	}

	public Token getToken() {
		return getCredential(TOKEN);
	}

	public void setToken(Token token) {
		setCredential(TOKEN, token);
	}

	public String getScope() {
		return getCredential(SCOPE);
	}

	public void setScope(String scope) {
		setCredential(SCOPE, scope);
	}

	public boolean isExpired() {
		Token token = getToken();
		return token instanceof ExpiringToken &&
			((ExpiringToken) token).isExpired();
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
			return isExpiring(node)
				? new ExpiringToken(VALUE.getValue(node), SECRET.getValue(node), EXPIRES.getValue(node), REFRESH.getValue(node))
				: new Token(VALUE.getValue(node), SECRET.getValue(node));
		}

		private boolean isExpiring(ObjectNode node) {
			return EXPIRES.getValue(node) != null;
		}

		@Override
		public JsonNode toJson(Token value) {
			if (value == null) {
				return NullNode.getInstance();
			}
			ObjectNode node = Nodes.newObject();
			VALUE.setValue(node, value.getToken());
			SECRET.setValue(node, value.getSecret());
			if (value instanceof ExpiringToken) {
				REFRESH.setValue(node, ((ExpiringToken) value).getRefreshToken());
				EXPIRES.setValue(node, ((ExpiringToken) value).getExpires());
			}
			return node;
		}
	}
}
