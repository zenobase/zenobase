package com.zenobase.tasks.fitbark;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class FitBarkCredentialsManager extends OAuthCredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(FitBarkCredentialsManager.class);

	private static final String TYPE = "fitbark";

	public FitBarkCredentialsManager(
			CredentialsRepository repository, String apiKey, String apiSecret, String callbackUrl) {
		super(TYPE, repository, new FitBarkApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	protected Token getRequestToken(OAuthCredentials credentials) {
		return Token.empty();
	}

	@Override
	public @Nullable Command authorize(Credentials credentials, ObjectNode config) {
		Preconditions.checkState(!credentials.isAuthorized());
		return authorize(credentials.as(OAuthCredentials.class), config);
	}

	private @Nullable Command authorize(OAuthCredentials credentials, ObjectNode config) {
		String code = config.path("code").textValue();
		if (code == null) {
			logger.warn("Couldn't obtain {} credentials <{}>: {}", credentials.getType(), credentials.getId(), config);
			return null;
		}
		Token token = getAccessToken(credentials, code);
		return UpdateCredentialsCommand.builder(credentials)
				.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), null)
				.with(Credentials.CREDENTIALS)
				.set(OAuthCredentials.TOKEN, credentials.getToken(), token)
				.build();
	}

	@Override
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		request.addHeader(
				"Authorization",
				"Bearer " + Objects.requireNonNull(credentials.getToken()).getToken());
	}
}
