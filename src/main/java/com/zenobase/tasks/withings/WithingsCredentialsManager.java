package com.zenobase.tasks.withings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WithingsCredentialsManager extends OAuthCredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(WithingsCredentialsManager.class);

	private static final String TYPE = "withings";

	@Inject
	public WithingsCredentialsManager(
		CredentialsRepository repository,
		@Named("withings.api.key") String apiKey,
		@Named("withings.api.secret") String apiSecret,
		@Named("oauth.hostname") String callbackUrl
	) {
		super(TYPE, repository, new WithingsApi(), apiKey, apiSecret, callbackUrl);
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
			logger.warn("Converting oauth1 token...");
			refreshToken = currentToken.getToken() + ":" + currentToken.getSecret();
		}
		var request = new OAuthRequest(Verb.POST, "https://wbsapi.withings.net/v2/oauth2");
		request.addBodyParameter("action", "requesttoken");
		request.addBodyParameter("grant_type", "refresh_token");
		request.addBodyParameter("client_id", getApiKey());
		request.addBodyParameter("client_secret", getApiSecret());
		request.addBodyParameter("refresh_token", refreshToken);
		Response response = request.send();
		if (response.isSuccessful()) {
			credentials.setToken(new WithingsAccessTokenExtractor().extract(response.getBody()));
		} else {
			logger.warn(
				"Couldn't refresh credentials {}: {} -> {}",
				credentials.getId(),
				response.getHeaders(),
				response.getBody()
			);
		}
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		request.addQuerystringParameter("access_token", Objects.requireNonNull(credentials.getToken()).getToken());
	}

	@Override
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}
}
