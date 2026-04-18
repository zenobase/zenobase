package com.zenobase.tasks;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
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

	public @Nullable Token getToken() {
		return getCredential(TOKEN);
	}

	public void setToken(Token token) {
		setCredential(TOKEN, token);
	}

	public @Nullable String getScope() {
		return getCredential(SCOPE);
	}

	public void setScope(String scope) {
		setCredential(SCOPE, scope);
	}

	public boolean isExpired() {
		return getToken() instanceof ExpiringToken token && token.isExpired();
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
				? new ExpiringToken(
						Objects.requireNonNull(VALUE.getValue(node)),
						Objects.requireNonNull(SECRET.getValue(node)),
						Objects.requireNonNull(EXPIRES.getValue(node)),
						Objects.requireNonNull(REFRESH.getValue(node))
					)
				: new Token(
						Objects.requireNonNull(VALUE.getValue(node)),
						Objects.requireNonNull(SECRET.getValue(node))
					);
		}

		private boolean isExpiring(ObjectNode node) {
			return EXPIRES.getValue(node) != null;
		}

		@Override
		public JsonNode toJson(@Nullable Token value) {
			if (value == null) {
				return NullNode.getInstance();
			}
			ObjectNode node = Nodes.newObject();
			VALUE.setValue(node, value.getToken());
			SECRET.setValue(node, value.getSecret());
			if (value instanceof ExpiringToken token) {
				REFRESH.setValue(node, token.getRefreshToken());
				EXPIRES.setValue(node, token.getExpires());
			}
			return node;
		}
	}
}
