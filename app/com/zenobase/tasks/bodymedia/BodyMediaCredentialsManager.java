package com.zenobase.tasks.bodymedia;

import javax.inject.Inject;
import javax.inject.Named;

import org.scribe.model.OAuthRequest;

import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class BodyMediaCredentialsManager extends OAuthCredentialsManager {

	public static final String TYPE = "bodymedia";

	@Inject
	public BodyMediaCredentialsManager(CredentialsRepository repository, @Named("bodymedia.api.key") String apiKey, @Named("bodymedia.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, repository, new BodyMediaApi(apiKey), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		request.addQuerystringParameter("api_key", getApiKey());
		super.sign(request, credentials);
	}

	@Override
	public void reauthorize(Credentials credentials) {
		reauthorize(credentials.as(OAuthCredentials.class));
	}

	private void reauthorize(OAuthCredentials credentials) {
		credentials.setToken(getAccessToken(credentials, ""));
	}
}
