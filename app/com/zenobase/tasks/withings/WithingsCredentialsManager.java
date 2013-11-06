package com.zenobase.tasks.withings;

import javax.inject.Inject;
import javax.inject.Named;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.SignatureType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;

public class WithingsCredentialsManager extends OAuthCredentialsManager {

	public static final String TYPE = "withings";

	@Inject
	public WithingsCredentialsManager(CredentialsRepository repository, @Named("withings.api.key") String apiKey, @Named("withings.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(TYPE, repository, new WithingsApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public Command authorize(Credentials credentials, ObjectNode config) {
		Preconditions.checkState(!credentials.isAuthorized());
		return authorize(credentials.as(OAuthCredentials.class), config);
	}

	private Command authorize(OAuthCredentials credentials, ObjectNode config) {

		String token = config.get("oauth_token").textValue();
		String verifier = config.get("oauth_verifier").textValue();
		String userId = Strings.emptyToNull(config.get("userid").asText());

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
}
