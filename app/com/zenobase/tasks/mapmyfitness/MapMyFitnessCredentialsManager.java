package com.zenobase.tasks.mapmyfitness;

import javax.inject.Inject;
import javax.inject.Named;

import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.oauth.OAuth2TokenExtractor;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class MapMyFitnessCredentialsManager extends OAuthCredentialsManager {

	private static final String TYPE = "mapmyfitness";

	@Inject
	public MapMyFitnessCredentialsManager(CredentialsRepository repository, @Named("mapmyfitness.api.key") String apiKey, @Named("mapmyfitness.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, repository, new MapMyFitnessApi(), apiKey, apiSecret, callbackUrl);
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
			Logger.warn(String.format("Couldn't obtain %s credentials <%s>: %s",
				credentials.getType(), credentials.getId(), config));
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
		OAuthRequest request = new OAuthRequest(Verb.POST, "https://oauth2-api.mapmyapi.com/v7.0/oauth2/access_token/");
		request.addBodyParameter("grant_type", "refresh_token");
		request.addBodyParameter("refresh_token", ((ExpiringToken) credentials.getToken()).getRefreshToken());
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
		request.addHeader("Authorization", "Bearer " + credentials.getToken().getToken());
		// request.addHeader("Accept-Encoding", "gzip");
	}
}
