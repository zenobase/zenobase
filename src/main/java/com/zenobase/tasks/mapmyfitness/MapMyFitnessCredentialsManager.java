package com.zenobase.tasks.mapmyfitness;

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

public class MapMyFitnessCredentialsManager extends OAuthCredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(MapMyFitnessCredentialsManager.class);

	private static final String TYPE = "mapmyfitness";

	@Inject
	public MapMyFitnessCredentialsManager(
		CredentialsRepository repository,
		@Named("mapmyfitness.api.key") String apiKey,
		@Named("mapmyfitness.api.secret") String apiSecret,
		@Named("api.hostname") String callbackUrl
	) {
		super(TYPE, repository, new MapMyFitnessApi(), apiKey, apiSecret, callbackUrl);
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
		ExpiringToken token = (ExpiringToken) getAccessToken(credentials, code);
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
		var request = new OAuthRequest(Verb.POST, MapMyFitnessApi.ACCESS_TOKEN_ENDPOINT);
		request.addHeader("Api-Key", getApiKey());
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
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		request.addHeader("Api-Key", getApiKey());
		request.addHeader("Authorization", "Bearer " + Objects.requireNonNull(credentials.getToken()).getToken());
	}
}
