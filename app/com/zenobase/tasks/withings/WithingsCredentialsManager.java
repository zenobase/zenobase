package com.zenobase.tasks.withings;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.oauth.OAuth2TokenExtractor;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class WithingsCredentialsManager extends OAuthCredentialsManager {

	private static final String TYPE = "withings";

	@Inject
	public WithingsCredentialsManager(CredentialsRepository repository, @Named("withings.api.key") String apiKey, @Named("withings.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, repository, new WithingsApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	protected Token getRequestToken(OAuthCredentials credentials) {
		return Token.empty();
	}

	@Override
	public Command authorize(Credentials credentials, ObjectNode config) {
		Preconditions.checkState(!credentials.isAuthorized());
		return authorize(credentials.as(OAuthCredentials.class), config);
	}

	private Command authorize(OAuthCredentials credentials, ObjectNode config) {
		String code = config.path("code").textValue();
		if (code == null) {
			Logger.warn("Couldn't obtain {} credentials <{}>: {}",
				credentials.getType(), credentials.getId(), config);
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
		if (credentials.getToken() instanceof ExpiringToken) {
			refreshToken = ((ExpiringToken) credentials.getToken()).getRefreshToken();
		} else {
			Logger.warn("Converting oauth1 token...");
			refreshToken = credentials.getToken().getToken() + ":" + credentials.getToken().getSecret();
		}
		OAuthRequest request = new OAuthRequest(Verb.POST, "https://wbsapi.withings.net/v2/oauth2");
		request.addBodyParameter("action", "requesttoken");
		request.addBodyParameter("grant_type", "refresh_token");
		request.addBodyParameter("client_id", getApiKey());
		request.addBodyParameter("client_secret", getApiSecret());
		request.addBodyParameter("refresh_token", refreshToken);
		Response response = request.send();
		if (response.isSuccessful()) {
			credentials.setToken(new WithingsAccessTokenExtractor().extract(response.getBody()));
		} else {
			Logger.warn("Couldn't refresh credentials {}: {} -> {}", credentials.getId(), response.getHeaders(), response.getBody());
		}
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		request.addQuerystringParameter("access_token", credentials.getToken().getToken());
	}

	@Override
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}
}
