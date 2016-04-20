package com.zenobase.tasks.ihealth;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class IHealthCredentialsManager extends OAuthCredentialsManager {

	public static final String TYPE = "ihealth";

	private final String callbackUrl;
	private final String sc;

	@Inject
	public IHealthCredentialsManager(CredentialsRepository integrations, @Named("ihealth.api.key") String apiKey, @Named("ihealth.api.secret") String apiSecret, @Named("ihealth.api.sc") String sc, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, integrations, new IHealthApi(), apiKey, apiSecret, callbackUrl);
		this.callbackUrl = callbackUrl;
		this.sc = sc;
	}

	@Override
	protected Token getRequestToken(OAuthCredentials integration) {
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
		IHealthToken token = (IHealthToken) getAccessToken(credentials, code);
		return UpdateCredentialsCommand.builder(credentials)
			.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), null)
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, credentials.getToken(), token)
			.set(OAuthCredentials.SCOPE, credentials.getScope(), token.getUserId())
			.build();
	}

	@Override
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		request.addQuerystringParameter("access_token", credentials.getToken().getToken());
		request.addQuerystringParameter("client_id", getApiKey());
		request.addQuerystringParameter("client_secret", getApiSecret());
		request.addQuerystringParameter("sc", sc);
	}

	@Override
	public void reauthorize(Credentials credentials) {
		reauthorize(credentials.as(OAuthCredentials.class));
	}

	private void reauthorize(OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, IHealthApi.ENDPOINT);
		request.addQuerystringParameter("UserID", credentials.getScope());
		request.addQuerystringParameter("redirect_uri", buildCallback(callbackUrl, credentials));
		request.addQuerystringParameter("response_type", "refresh_token");
		request.addQuerystringParameter("refresh_token", ((ExpiringToken) credentials.getToken()).getRefreshToken());
		request.addQuerystringParameter(OAuthConstants.CLIENT_ID, getApiKey());
		request.addQuerystringParameter(OAuthConstants.CLIENT_SECRET, getApiSecret());
		credentials.setToken(new IHealthTokenExtractor().extract(request.send().getBody()));
	}
}
