package com.zenobase.tasks.goodreads;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jspecify.annotations.Nullable;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class GoodreadsCredentialsManager extends OAuthCredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(GoodreadsCredentialsManager.class);

	public static final String TYPE = "goodreads";

	@Inject
	public GoodreadsCredentialsManager(
		CredentialsRepository integrations,
		@Named("goodreads.api.key") String apiKey,
		@Named("goodreads.api.secret") String apiSecret,
		@Named("oauth.hostname") String callbackUrl
	) {
		super(TYPE, integrations, new GoodreadsApi(callbackUrl + "/oauth/callback/-"), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public @Nullable Command authorize(Credentials credentials, ObjectNode config) {
		Preconditions.checkState(!credentials.isAuthorized());
		return authorize(credentials.as(OAuthCredentials.class), config);
	}

	private @Nullable Command authorize(OAuthCredentials credentials, ObjectNode config) {
		int result = config.path("authorize").asInt();
		String code = config.path("oauth_token").textValue();
		if (code == null || result != 1) {
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
	protected void configure(ServiceBuilder builder) {
		builder.signatureType(SignatureType.QueryString);
	}
}
