package com.zenobase.tasks.nokia;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zenobase.models.Identity;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.SignatureType;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class NokiaHealthCredentialsManager extends OAuthCredentialsManager {

	private static final String TYPE = "nokia";

	@Inject
	public NokiaHealthCredentialsManager(CredentialsRepository repository, @Named("nokia.api.key") String apiKey, @Named("nokia.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, repository, new NokiaHealthApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public Command authorize(Credentials credentials, ObjectNode config) {
		Preconditions.checkState(!credentials.isAuthorized());
		return authorize(credentials.as(OAuthCredentials.class), config);
	}

	private Command authorize(OAuthCredentials credentials, ObjectNode config) {

		String token = config.path("oauth_token").textValue();
		String verifier = config.path("oauth_verifier").textValue();
		String userId = Strings.emptyToNull(config.path("userid").asText());

		Preconditions.checkNotNull(token);
		Preconditions.checkNotNull(verifier);
		Preconditions.checkState(credentials.getToken().getToken().equals(token),
			"Expected token <%s> but got <%s> in credentials <%s>",
			credentials.getToken().getToken(), token, credentials.getId());

		return UpdateCredentialsCommand.builder(credentials)
			.set(Credentials.AUTHORIZATION_URL, credentials.getAuthorizationUrl(), null)
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, credentials.getToken(), getAccessToken(credentials, verifier))
			.set(OAuthCredentials.SCOPE, credentials.getScope(), userId)
			.build();
	}

	@Override
	protected void configure(ServiceBuilder builder) {
		builder.signatureType(SignatureType.QueryString);
	}

	@Override
	protected Credentials find(Identity principal) {
		Credentials credentials = super.find(principal, TYPE);
		if (credentials == null) { // TODO remove after the next migration
			credentials = super.find(principal, "withings");
		}
		return credentials;
	}
}
