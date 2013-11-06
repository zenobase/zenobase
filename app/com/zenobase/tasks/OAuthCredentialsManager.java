package com.zenobase.tasks;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.models.Identity;
import com.zenobase.services.CredentialsRepository;

public abstract class OAuthCredentialsManager extends CredentialsManager {

	private final CredentialsRepository repository;
	private final Api provider;
	private final String apiKey;
	private final String apiSecret;
	private final String callbackUrl;

	protected OAuthCredentialsManager(String type, CredentialsRepository repository, Api provider, String apiKey, String apiSecret, String callbackUrl) {
		super(type);
		this.repository = repository;
		this.provider = provider;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.callbackUrl = callbackUrl;
	}

	protected String getApiKey() {
		return apiKey;
	}

	protected String getApiSecret() {
		return apiSecret;
	}

	protected Credentials find(Identity principal) {
		return repository.find(principal, getType());
	}

	@Override
	public OAuthCredentials newCredentials(Identity principal) {
		OAuthCredentials credentials = new OAuthCredentials(getType(), principal);
		credentials.setToken(getRequestToken(credentials));
		credentials.setAuthorizationUrl(getService(credentials).getAuthorizationUrl(credentials.getToken()));
		return credentials;
	}

	protected Token getRequestToken(OAuthCredentials credentials) {
		return getService(credentials).getRequestToken();
	}

	@Override
	public Command authorize(Credentials credentials, ObjectNode config) {
		return config.size() != 0 ?
			authorize(credentials.as(OAuthCredentials.class), config) :
			deauthorize(credentials.as(OAuthCredentials.class));
	}

	private Command authorize(OAuthCredentials credentials, ObjectNode config) {
		Preconditions.checkState(!credentials.isAuthorized());
		String token = config.path("oauth_token").textValue();
		String verifier = config.path("oauth_verifier").asText();
		if (token == null) {
			Logger.warn(String.format("Couldn't authorize %s credentials<%s>: %s",
				credentials.getType(), credentials.getId(), config));
			return null;
		}
		Preconditions.checkState(credentials.getToken().getToken().equals(token),
			"Token matches in credentials %s, expected %s, got %s",
			credentials.getId(), credentials.getToken().getToken(), token);
		return UpdateCredentialsCommand.builder(credentials)
			.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), null)
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, credentials.getToken(), getAccessToken(credentials, verifier))
			.build();
	}

	public void reauthorize(Credentials credentials) {
		throw new UnsupportedOperationException();
	}

	private Command deauthorize(OAuthCredentials credentials) {
		Token token = getRequestToken(credentials);
		return UpdateCredentialsCommand.builder(credentials)
			.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), getService(credentials).getAuthorizationUrl(token))
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, credentials.getToken(), token)
			.build();
	}

	protected Token getAccessToken(OAuthCredentials credentials, String verifier) {
		return getService(credentials).getAccessToken(credentials.getToken(), new Verifier(verifier));
	}

	public Response send(OAuthRequest request, OAuthCredentials credentials) {
		sign(request, credentials);
		return request.send();

	}

	protected void sign(OAuthRequest request, OAuthCredentials credentials) {
		getService(credentials).signRequest(credentials.getToken(), request);
	}

	protected OAuthService getService(OAuthCredentials credentials) {
		ServiceBuilder builder = new ServiceBuilder()
			.provider(provider)
			.apiKey(apiKey)
			.apiSecret(apiSecret)
			.callback(String.format("%s/oauth/callback/%s", callbackUrl, credentials.getId()));
		configure(builder);
		return builder.build();
	}

	protected void configure(ServiceBuilder builder) {

	}
}
