package com.zenobase.tasks.withings;

import static com.zenobase.tasks.RecordingTransport.bodyParams;
import static com.zenobase.tasks.RecordingTransport.queryParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.OAuth2CredentialsManagerTestSupport;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

public class WithingsCredentialsManagerTest extends OAuth2CredentialsManagerTestSupport {

	private final WithingsCredentialsManager manager = new WithingsCredentialsManager(
		mock(CredentialsRepository.class),
		"client",
		"secret",
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
		return "https://wbsapi.withings.net/v2/oauth2";
	}

	@Override
	protected String tokenResponse() {
		// Withings wraps the token in a status envelope, and signals errors in the status rather than the HTTP code
		return """
		{"status": 0, "body": %s}
		""".formatted(TOKEN_RESPONSE);
	}

	@Override
	protected void assertTokenRequest(OAuthRequest request, Map<String, String> params, OAuthCredentials credentials) {
		super.assertTokenRequest(request, params, credentials);
		assertThat(params).containsEntry("action", "requesttoken");
	}

	@Test
	public void reauthorizeExchangesRefreshToken() {
		OAuthRequest request = refreshExpiredToken();

		assertThat(bodyParams(request))
			.containsEntry("action", "requesttoken")
			.containsEntry("client_id", "client")
			.containsEntry("client_secret", "secret");
	}

	@Test
	public void reauthorizeConvertsOAuth1TokenIntoRefreshToken() {
		OAuthCredentials credentials = authorizedCredentials(new Token("key", "secret"));
		transport.reply(200, tokenResponse());

		manager.reauthorize(credentials);

		assertThat(bodyParams(transport.onlyRequest())).containsEntry("refresh_token", "key:secret");
		assertNewToken(credentials);
	}

	@Test
	public void reauthorizeKeepsTokenWhenRefreshFails() {
		OAuthCredentials credentials = authorizedCredentials(expiredToken());
		transport.reply(503, "Service Unavailable");

		manager.reauthorize(credentials);

		assertThat(credentials.getToken().getToken()).isEqualTo("old-access");
	}

	@Test
	public void reauthorizeRejectsErrorStatus() {
		// OAuthTaskManager turns this into an InvalidTokenException, which resets the credentials
		OAuthCredentials credentials = authorizedCredentials(expiredToken());
		transport.reply(
			200,
			"""
			{"status": 401, "body": {}}
			"""
		);

		assertThatThrownBy(() -> manager.reauthorize(credentials)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void sendPassesTokenAsQueryParameter() {
		OAuthRequest request = sendWithToken();

		assertThat(queryParams(request)).containsEntry("access_token", "t");
		assertThat(request.getHeaders()).doesNotContainKey("Authorization");
	}
}
