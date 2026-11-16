package com.zenobase.tasks.trakt;

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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraktCredentialsManager extends OAuthCredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(TraktCredentialsManager.class);

	public static final String TYPE = "trakt";

	private final String callbackUrl;

	@Inject
	public TraktCredentialsManager(
		CredentialsRepository integrations,
		@Named("trakt.api.key") String apiKey,
		@Named("api.hostname") String callbackUrl
	) {
		super(TYPE, integrations, new TraktApi(), apiKey, "", callbackUrl);
		this.callbackUrl = callbackUrl;
	}

	/** Trakt uses PKCE instead of a client secret; the pending token's secret holds the code verifier. */
	@Override
	protected Token getRequestToken(OAuthCredentials credentials) {
		return new Token("", newCodeVerifier());
	}

	@Override
	public String getAuthorizationUrl(OAuthCredentials credentials, Token requestToken) {
		return TraktApi.getAuthorizationUrl(
			getApiKey(),
			buildCallback(callbackUrl, credentials),
			codeChallenge(requestToken.getSecret())
		);
	}

	static String newCodeVerifier() {
		var bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String codeChallenge(String verifier) {
		try {
			var digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected Token getAccessToken(OAuthCredentials credentials, String code) {
		var request = new OAuthRequest(Verb.POST, "https://api.trakt.tv/oauth/token");
		request.addHeader("User-Agent", "zeno");
		request.addHeader("Accept", "application/json");
		request.addBodyParameter("grant_type", "authorization_code");
		request.addBodyParameter(OAuthConstants.CODE, code);
		request.addBodyParameter(OAuthConstants.REDIRECT_URI, buildCallback(callbackUrl, credentials));
		request.addBodyParameter(OAuthConstants.CLIENT_ID, getApiKey());
		request.addBodyParameter("code_verifier", Objects.requireNonNull(credentials.getToken()).getSecret());
		return new OAuth2TokenExtractor().extract(request.send().getBody());
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
		var request = new OAuthRequest(Verb.POST, "https://api.trakt.tv/oauth/token");
		request.addHeader("trakt-api-version", "2");
		request.addHeader("trakt-api-key", getApiKey());
		request.addBodyParameter("grant_type", "refresh_token");
		request.addBodyParameter(
			"refresh_token",
			((ExpiringToken) Objects.requireNonNull(credentials.getToken())).getRefreshToken()
		);
		request.addBodyParameter(OAuthConstants.CLIENT_ID, getApiKey());
		credentials.setToken(new OAuth2TokenExtractor().extract(request.send().getBody()));
	}

	@Override
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		request.addHeader("Content-Type", "application/json");
		request.addHeader("trakt-api-version", "2");
		request.addHeader("trakt-api-key", getApiKey());
		request.addHeader("Authorization", "Bearer " + Objects.requireNonNull(credentials.getToken()).getToken());
	}
}
