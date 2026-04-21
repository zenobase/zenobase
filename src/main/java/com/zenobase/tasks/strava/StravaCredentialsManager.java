package com.zenobase.tasks.strava;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.oauth.OAuth2TokenExtractor;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StravaCredentialsManager extends OAuthCredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(StravaCredentialsManager.class);

	public static final String TYPE = "strava";

	@Inject
	public StravaCredentialsManager(
		CredentialsRepository integrations,
		@Named("strava.api.key") String apiKey,
		@Named("strava.api.secret") String apiSecret,
		@Named("web.hostname") String callbackUrl
	) {
		super(TYPE, integrations, new StravaApi(), apiKey, apiSecret, callbackUrl);
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
		var request = new OAuthRequest(Verb.POST, "https://www.strava.com/oauth/token");
		request.addBodyParameter("grant_type", "refresh_token");
		request.addBodyParameter(
			"refresh_token",
			((ExpiringToken) Objects.requireNonNull(credentials.getToken())).getRefreshToken()
		);
		request.addBodyParameter(OAuthConstants.CLIENT_ID, getApiKey());
		request.addBodyParameter(OAuthConstants.CLIENT_SECRET, getApiSecret());
		credentials.setToken(new OAuth2TokenExtractor().extract(request.send().getBody()));
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		request.addHeader("Authorization", "Bearer " + Objects.requireNonNull(credentials.getToken()).getToken());
	}
}
