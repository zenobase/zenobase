package com.zenobase.tasks.trakt;

import static com.zenobase.tasks.RecordingTransport.bodyParams;
import static com.zenobase.tasks.RecordingTransport.queryParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zenobase.models.Identity;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.OAuth2CredentialsManagerTestSupport;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

public class TraktCredentialsManagerTest extends OAuth2CredentialsManagerTestSupport {

	private final TraktCredentialsManager manager = new TraktCredentialsManager(
		mock(CredentialsRepository.class),
		"client",
		HOST
	) {
		@Override
		protected Response execute(OAuthRequest request) {
			return transport.execute(request);
		}
	};

	@Override
	protected OAuthCredentialsManager manager() {
		return manager;
	}

	@Override
	protected String tokenEndpoint() {
		return "https://api.trakt.tv/oauth/token";
	}

	/** PKCE: the code verifier stands in for the client secret. */
	@Override
	protected void assertTokenRequest(OAuthRequest request, Map<String, String> params, OAuthCredentials credentials) {
		assertThat(params)
			.containsEntry("client_id", "client")
			.containsEntry("code_verifier", credentials.getToken().getSecret())
			.doesNotContainKey("client_secret");
	}

	@Test
	public void newCredentialsUsesPkce() {
		var credentials = manager.newCredentials(new Identity());

		var verifier = credentials.getToken().getSecret();
		assertThat(verifier).matches("[A-Za-z0-9_-]{43}");
		var url = URI.create(credentials.getAuthorizationUrl());
		assertThat(url.getHost()).isEqualTo("auth.trakt.tv");
		assertThat(queryParams(url))
			.containsEntry("client_id", "client")
			.containsEntry("code_challenge", TraktCredentialsManager.codeChallenge(verifier))
			.containsEntry("code_challenge_method", "S256");
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

	@Test
	public void reauthorizeExchangesRefreshTokenWithoutSecret() {
		OAuthRequest request = refreshExpiredToken();

		assertThat(request.getHeaders())
			.containsEntry("trakt-api-version", "2")
			.containsEntry("trakt-api-key", "client");
		assertThat(bodyParams(request)).containsEntry("client_id", "client").doesNotContainKey("client_secret");
	}

	@Test
	public void reauthorizeThrowsInvalidTokenOnlyWhenRejected() {
		assertRefreshFailures();
	}

	@Test
	public void sendPassesApiKeyAndBearerHeaders() {
		assertThat(sendWithToken().getHeaders())
			.containsEntry("trakt-api-version", "2")
			.containsEntry("trakt-api-key", "client")
			.containsEntry("Authorization", "Bearer t");
	}
}
