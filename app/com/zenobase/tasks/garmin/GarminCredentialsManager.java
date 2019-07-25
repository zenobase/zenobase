package com.zenobase.tasks.garmin;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.Token;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class GarminCredentialsManager extends OAuthCredentialsManager {

	public static final String TYPE = "garmin";

	@Inject
	public GarminCredentialsManager(CredentialsRepository integrations, @Named("garmin.api.key") String apiKey, @Named("garmin.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, integrations, new GarminApi(callbackUrl + "/oauth/callback/-"), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public Command authorize(Credentials credentials, ObjectNode config) {
		Preconditions.checkState(!credentials.isAuthorized());
		return authorize(credentials.as(OAuthCredentials.class), config);
	}

	private Command authorize(OAuthCredentials credentials, ObjectNode config) {
		String verifier = config.path("oauth_verifier").textValue();
		if ("NULL".equals(verifier)) {
			Logger.warn("Couldn't obtain {} credentials <{}>: {}",
				credentials.getType(), credentials.getId(), config);
			return null;
		}
		Token token = getAccessToken(credentials, verifier);
		return UpdateCredentialsCommand.builder(credentials)
			.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), null)
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, credentials.getToken(), token)
			.build();
	}

	@Override
	protected void configure(ServiceBuilder builder) {

	}
}
