package com.zenobase.tasks.hexoskin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.oauth.OAuth2TokenExtractor;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CustomApi20;
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

public class HexoskinCredentialsManager extends OAuthCredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(HexoskinCredentialsManager.class);

	private static final String TYPE = "hexoskin";

	@Inject
	public HexoskinCredentialsManager(
		CredentialsRepository repository,
		@Named("hexoskin.api.key") String apiKey,
		@Named("hexoskin.api.secret") String apiSecret,
		@Named("api.hostname") String callbackUrl
	) {
		super(TYPE, repository, new HexoskinApi(), apiKey, apiSecret, callbackUrl);
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
		if (credentials.getToken() instanceof ExpiringToken token) {
			refreshToken = token.getRefreshToken();
		} else {
			Token t = Objects.requireNonNull(credentials.getToken());
			refreshToken = t.getToken() + ":" + t.getSecret();
		}
		var request = new OAuthRequest(Verb.POST, "https://api.hexoskin.com/api/connect/oauth2/token/");
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
				response.getBody()
			);
		}
	}

	@Override
	protected Token getRequestToken(OAuthCredentials integration) {
		return Token.empty();
	}

	@Override
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}
}
