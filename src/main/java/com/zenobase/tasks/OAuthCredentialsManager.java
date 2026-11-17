package com.zenobase.tasks;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.models.Identity;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.oauth.OAuth2TokenExtractor;
import com.zenobase.repositories.CredentialsRepository;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		String callbackUrl
	) {
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
		credentials.setAuthorizationUrl(
			getAuthorizationUrl(credentials, Objects.requireNonNull(credentials.getToken()))
		);
		return credentials;
	}

	public String getAuthorizationUrl(OAuthCredentials credentials, Token requestToken) {
		return getService(credentials).getAuthorizationUrl(requestToken);
	}

	protected Token getRequestToken(OAuthCredentials credentials) {
		return getService(credentials).getRequestToken();
	}

	@Override
	public @Nullable Command authorize(Credentials untyped, ObjectNode config) {
		OAuthCredentials credentials = untyped.as(OAuthCredentials.class);
		Preconditions.checkState(!credentials.isAuthorized());
		String token = config.path("oauth_token").textValue();
		String verifier = config.path("oauth_verifier").asText();
		if (token == null) {
			logger.warn("Couldn't obtain {} credentials <{}>: {}", credentials.getType(), credentials.getId(), config);
			return null;
		}
		checkRequestToken(credentials, token);
		return UpdateCredentialsCommand.builder(credentials)
			.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), null)
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, credentials.getToken(), getAccessToken(credentials, verifier))
			.build();
	}

	protected static void checkRequestToken(OAuthCredentials credentials, String token) {
		Token requestToken = Objects.requireNonNull(credentials.getToken());
		Preconditions.checkState(
			requestToken.getToken().equals(token),
			"Token mismatch in credentials %s",
			credentials.getId()
		);
	}

	public void reauthorize(Credentials credentials) {
		throw new UnsupportedOperationException();
	}

	protected Token getAccessToken(OAuthCredentials credentials, String verifier) {
		return getService(credentials).getAccessToken(
			Objects.requireNonNull(credentials.getToken()),
			new Verifier(verifier)
		);
	}

	/**
	 * Sends a refresh token request. A 400 or 401 response means the provider rejected the refresh token, so it
	 * throws {@link InvalidTokenException}; any other failure, such as an outage, throws {@link InvalidStatusException}.
	 */
	protected ExpiringToken refresh(OAuthRequest request, OAuthCredentials credentials) {
		Response response = execute(request);
		if (response.isSuccessful()) {
			return new OAuth2TokenExtractor().extract(response.getBody());
		}
		if (response.getCode() == 400 || response.getCode() == 401) {
			throw new InvalidTokenException(credentials);
		}
		throw new InvalidStatusException(request.getCompleteUrl(), response.getCode(), response.getBody());
	}

	public Response send(OAuthRequest request, OAuthCredentials credentials) {
		sign(request, credentials);
		return execute(request);
	}

	/**
	 * Sends a request over the network. CustomApi20 token exchanges go through here too, but Scribe's OAuth 1.0a token
	 * requests (Goodreads) don't.
	 */
	protected Response execute(OAuthRequest request) {
		return request.send();
	}

	protected void sign(OAuthRequest request, OAuthCredentials credentials) {
		getService(credentials).signRequest(Objects.requireNonNull(credentials.getToken()), request);
	}

	protected OAuthService getService(OAuthCredentials credentials) {
		var builder = new ServiceBuilder()
			.provider(this::createService)
			.apiKey(apiKey)
			.apiSecret(apiSecret)
			.callback(buildCallback(callbackUrl, credentials));
		configure(builder);
		return builder.build();
	}

	private OAuthService createService(OAuthConfig config) {
		return provider instanceof CustomApi20 api
			? api.createService(config, this::execute)
			: provider.createService(config);
	}

	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/" + credentials.getId();
	}

	protected void configure(ServiceBuilder builder) {}
}
