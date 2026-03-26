package com.zenobase.tasks.rescuetime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class RescueTimeCredentialsManager extends OAuthCredentialsManager {

	private static final Logger logger = LoggerFactory.getLogger(RescueTimeCredentialsManager.class);

	public static final String TYPE = "rescuetime";

	@Inject
	public RescueTimeCredentialsManager(
			CredentialsRepository integrations,
			@Named("rescuetime.api.key") String apiKey,
			@Named("rescuetime.api.secret") String apiSecret,
			@Named("oauth.hostname") String callbackUrl) {
		super(TYPE, integrations, new RescueTimeApi(), apiKey, apiSecret, callbackUrl);
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
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		request.addQuerystringParameter("access_token", credentials.getToken().getToken());
	}
}
