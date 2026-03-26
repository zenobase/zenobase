package com.zenobase.tasks.fitbit;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.oauth.OAuth2TokenExtractor;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CustomApi20;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class FitbitCredentialsManager extends OAuthCredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(FitbitCredentialsManager.class);

	private static final String TYPE = "fitbit";

	@Inject
	public FitbitCredentialsManager(
			CredentialsRepository repository,
			@Named("fitbit.api.key") String apiKey,
			@Named("fitbit.api.secret") String apiSecret,
			@Named("oauth.hostname") String callbackUrl) {
		super(TYPE, repository, new FitbitApi(), apiKey, apiSecret, callbackUrl);
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
	public void reauthorize(Credentials credentials) {
		reauthorize(credentials.as(OAuthCredentials.class));
	}

	private void reauthorize(OAuthCredentials credentials) {
		String refreshToken;
		Token currentToken = Objects.requireNonNull(credentials.getToken());
		if (currentToken instanceof ExpiringToken token) {
			refreshToken = token.getRefreshToken();
		} else {
			refreshToken = currentToken.getToken() + ":" + currentToken.getSecret();
		}
		var request = new OAuthRequest(Verb.POST, "https://api.fitbit.com/oauth2/token");
		request.addBodyParameter("grant_type", "refresh_token");
		request.addBodyParameter("refresh_token", refreshToken);
		CustomApi20.addBasicAuthHeader(request, getApiKey(), getApiSecret());
		Response response = request.send();
		if (response.isSuccessful()) {
			credentials.setToken(new OAuth2TokenExtractor().extract(response.getBody()));
		} else {
			logger.warn(
					"Couldn't refresh credentials {}: {} -> {}",
					credentials.getId(),
					response.getHeaders(),
					response.getBody());
		}
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
