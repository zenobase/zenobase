package com.zenobase.tasks.lastfm;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.json.Nodes;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class LastFmCredentialsManager extends OAuthCredentialsManager {

	private static final String TYPE = "lastfm";

	private final String apiKey;
	private final Signature signature;

	@Inject
	public LastFmCredentialsManager(CredentialsRepository repository, @Named("lastfm.api.key") String apiKey, @Named("lastfm.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, repository, new LastFmApi(), apiKey, apiSecret, callbackUrl);
		this.apiKey = apiKey;
		this.signature = new Signature(apiSecret);
	}

	@Override
	protected Token getRequestToken(OAuthCredentials credentials) {
		return Token.empty();
	}

	@Override
	public Command authorize(Credentials credentials, ObjectNode config) {
		Preconditions.checkState(!credentials.isAuthorized());
		return authorize(credentials.as(OAuthCredentials.class), config);
	}

	private Command authorize(OAuthCredentials credentials, ObjectNode config) {
		String code = config.path("token").textValue();
		if (code == null) {
			Logger.warn("Couldn't obtain {} credentials <{}>: {}",
				credentials.getType(), credentials.getId(), config);
			return null;
		}
		LastFmToken token = getAccessToken(credentials, code);
		return UpdateCredentialsCommand.builder(credentials)
			.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), null)
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, credentials.getToken(), token)
			.set(OAuthCredentials.SCOPE, credentials.getScope(), token.getScope())
			.build();
	}

	@Override
	protected LastFmToken getAccessToken(OAuthCredentials credentials, String verifier) {
		LastFmRequest request = new LastFmRequest();
		request.addQuerystringParameter("method", "auth.getSession");
		request.addQuerystringParameter("token", verifier);
		request.addQuerystringParameter("api_key", apiKey);
		request.addQuerystringParameter("api_sig", signature.sign(request.getQuerystringParameters()));
		request.addQuerystringParameter("format", "json");
		Response response = request.send();
		ObjectNode result = Nodes.readObject(response.getBody());
		String token = result.path("session").path("key").textValue();
		String scope = result.path("session").path("name").textValue();
		Preconditions.checkNotNull(token, "Expected a key: " + result);
		Preconditions.checkNotNull(scope, "Expected a name: " + result);
		return new LastFmToken(token, scope);
	}

	@Override
	public void sign(OAuthRequest request, OAuthCredentials credentials) {
		sign((LastFmRequest) request, credentials);
	}

	private void sign(LastFmRequest request, OAuthCredentials credentials) {
		request.addQuerystringParameter("sk", credentials.getToken().getToken());
		request.addQuerystringParameter("api_key", apiKey);
		request.addQuerystringParameter("api_sig", signature.sign(request.getQuerystringParameters()));
	}
}
