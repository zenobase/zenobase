package com.zenobase.tasks.beeminder;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class BeeminderCredentialsManager extends OAuthCredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(BeeminderCredentialsManager.class);

	public static final String TYPE = "beeminder";

	public BeeminderCredentialsManager(
			CredentialsRepository integrations, String apiKey, String apiSecret, String callbackUrl) {
		super(TYPE, integrations, new BeeminderApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	protected Token getRequestToken(OAuthCredentials integration) {
		return Token.empty();
	}

	@Override
	public @Nullable Command authorize(Credentials credentials, ObjectNode config) {
		Preconditions.checkState(!credentials.isAuthorized());
		return authorize(credentials.as(OAuthCredentials.class), config);
	}

	private @Nullable Command authorize(OAuthCredentials credentials, ObjectNode config) {
		String code = config.path("access_token").textValue();
		if (code == null) {
			logger.warn("Couldn't obtain {} credentials <{}>: {}", credentials.getType(), credentials.getId(), config);
			return null;
		}
		return UpdateCredentialsCommand.builder(credentials)
				.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), null)
				.with(Credentials.CREDENTIALS)
				.set(OAuthCredentials.TOKEN, credentials.getToken(), new Token(code, ""))
				.build();
	}

	@Override
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		if (request.getVerb() == Verb.POST) {
			request.addBodyParameter(
					"access_token",
					Objects.requireNonNull(credentials.getToken()).getToken());
		} else if (request.getVerb() == Verb.GET) {
			request.addQuerystringParameter(
					"access_token",
					Objects.requireNonNull(credentials.getToken()).getToken());
		} else {
			throw new UnsupportedOperationException();
		}
	}
}
