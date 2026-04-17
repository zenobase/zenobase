package com.zenobase.tasks;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.models.Identity;
import com.zenobase.repositories.CredentialsRepository;

public abstract class OAuthCredentialsManager extends CredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(OAuthCredentialsManager.class);

	private final CredentialsRepository repository;
	private final Api provider;
	private final String apiKey;
	private final String apiSecret;
	private final String callbackUrl;

	protected OAuthCredentialsManager(
			String type,
			CredentialsRepository repository,
			Api provider,
			String apiKey,
			String apiSecret,
			String callbackUrl) {
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

	protected @Nullable Credentials find(Identity principal) {
		return find(principal, getType());
	}

	protected @Nullable Credentials find(Identity principal, String type) {
		return repository.find(principal, type);
	}

	@Override
	public OAuthCredentials newCredentials(Identity principal) {
		var credentials = new OAuthCredentials(getType(), principal);
		credentials.setToken(getRequestToken(credentials));
		credentials.setAuthorizationUrl(getService(credentials).getAuthorizationUrl(credentials.getToken()));
		return credentials;
	}

	protected Token getRequestToken(OAuthCredentials credentials) {
		return getService(credentials).getRequestToken();
	}

	@Override
	public @Nullable Command authorize(Credentials credentials, ObjectNode config) {
		return !config.isEmpty()
				? authorize(credentials.as(OAuthCredentials.class), config)
				: deauthorize(credentials.as(OAuthCredentials.class));
	}

	private @Nullable Command authorize(OAuthCredentials credentials, ObjectNode config) {
		Preconditions.checkState(!credentials.isAuthorized());
		String token = config.path("oauth_token").textValue();
		String verifier = config.path("oauth_verifier").asText();
		if (token == null) {
			logger.warn("Couldn't obtain {} credentials <{}>: {}", credentials.getType(), credentials.getId(), config);
			return null;
		}
		Token credentialsToken = Objects.requireNonNull(credentials.getToken());
		Preconditions.checkState(
				credentialsToken.getToken().equals(token),
				"Token matches in credentials %s, expected %s, got %s",
				credentials.getId(),
				credentialsToken.getToken(),
				token);
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
				.set(
						Credentials.AUTHORIZATION_URL,
						credentials.getAuthorizationUrl(),
						getService(credentials).getAuthorizationUrl(token))
				.with(Credentials.CREDENTIALS)
				.set(OAuthCredentials.TOKEN, credentials.getToken(), token)
				.build();
	}

	protected Token getAccessToken(OAuthCredentials credentials, String verifier) {
		return getService(credentials)
				.getAccessToken(Objects.requireNonNull(credentials.getToken()), new Verifier(verifier));
	}

	public Response send(OAuthRequest request, OAuthCredentials credentials) {
		sign(request, credentials);
		return request.send();
	}

	protected void sign(OAuthRequest request, OAuthCredentials credentials) {
		getService(credentials).signRequest(Objects.requireNonNull(credentials.getToken()), request);
	}

	protected OAuthService getService(OAuthCredentials credentials) {
		var builder = new ServiceBuilder()
				.provider(provider)
				.apiKey(apiKey)
				.apiSecret(apiSecret)
				.callback(buildCallback(callbackUrl, credentials));
		configure(builder);
		return builder.build();
	}

	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/" + credentials.getId();
	}

	protected void configure(ServiceBuilder builder) {}
}
