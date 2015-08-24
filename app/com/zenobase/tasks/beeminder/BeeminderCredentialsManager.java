package com.zenobase.tasks.beeminder;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class BeeminderCredentialsManager extends OAuthCredentialsManager {

	public static final String TYPE = "beeminder";

	@Inject
	public BeeminderCredentialsManager(CredentialsRepository integrations, @Named("beeminder.api.key") String apiKey, @Named("beeminder.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, integrations, new BeeminderApi(), apiKey, apiSecret, callbackUrl);
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
		String code = config.path("access_token").textValue();
		if (code == null) {
			Logger.warn("Couldn't obtain {} credentials <{}>: {}",
				credentials.getType(), credentials.getId(), config);
			return null;
		}
		return UpdateCredentialsCommand.builder(credentials)
			.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), null)
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, credentials.getToken(), new Token(code, ""))
			.build();
	}

	@Override
	protected String buildCallback(String baseUrl, OAuthCredentials credentials) {
		return baseUrl + "/oauth/callback/-";
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		if (request.getVerb() == Verb.POST) {
			request.addBodyParameter("access_token", credentials.getToken().getToken());
		} else if (request.getVerb() == Verb.GET) {
			request.addQuerystringParameter("access_token", credentials.getToken().getToken());
		} else {
			throw new UnsupportedOperationException();
		}
	}
}
