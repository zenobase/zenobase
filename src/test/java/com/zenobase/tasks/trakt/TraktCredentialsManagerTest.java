package com.zenobase.tasks.trakt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zenobase.models.Identity;
import com.zenobase.repositories.CredentialsRepository;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class TraktCredentialsManagerTest {

	private final TraktCredentialsManager manager = new TraktCredentialsManager(
		mock(CredentialsRepository.class),
		"client",
		"https://api.example.com"
	);

	@Test
	public void newCredentialsUsesPkce() {
		var credentials = manager.newCredentials(new Identity());

		var verifier = credentials.getToken().getSecret();
		assertThat(verifier).matches("[A-Za-z0-9_-]{43}");
		var url = URI.create(credentials.getAuthorizationUrl());
		assertThat(url.getHost()).isEqualTo("auth.trakt.tv");
		var query = URLDecoder.decode(url.getRawQuery(), StandardCharsets.UTF_8);
		assertThat(query)
			.contains("client_id=client")
			.contains("redirect_uri=https://api.example.com/oauth/callback/-")
			.contains("code_challenge=" + TraktCredentialsManager.codeChallenge(verifier))
			.contains("code_challenge_method=S256");
	}

	@Test
	public void invalidTokenRecoveryIssuesFreshPkceUrl() {
		var credentials = manager.newCredentials(new Identity());
		var requestToken = manager.getRequestToken(credentials);

		assertThat(requestToken.getSecret())
			.matches("[A-Za-z0-9_-]{43}")
			.isNotEqualTo(credentials.getToken().getSecret());
		assertThat(manager.getAuthorizationUrl(credentials, requestToken)).contains(
			"code_challenge=" + TraktCredentialsManager.codeChallenge(requestToken.getSecret())
		);
	}

	@Test
	public void codeChallengeMatchesRfc7636Example() {
		assertThat(TraktCredentialsManager.codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")).isEqualTo(
			"E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
		);
	}
}
