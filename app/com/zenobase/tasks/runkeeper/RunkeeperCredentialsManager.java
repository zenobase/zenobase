package com.zenobase.tasks.runkeeper;

import javax.inject.Inject;
import javax.inject.Named;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class RunkeeperCredentialsManager extends OAuthCredentialsManager {

	public static final String TYPE = "runkeeper";

	@Inject
	public RunkeeperCredentialsManager(CredentialsRepository integrations, @Named("runkeeper.api.key") String apiKey, @Named("runkeeper.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, integrations, new RunkeeperApi(), apiKey, apiSecret, callbackUrl);
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
		Token token = getAccessToken(credentials, code);
		return UpdateCredentialsCommand.builder(credentials)
			.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), null)
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, credentials.getToken(), token)
			.build();
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		request.addHeader("Authorization", "Bearer " + credentials.getToken().getToken());
	}
}
