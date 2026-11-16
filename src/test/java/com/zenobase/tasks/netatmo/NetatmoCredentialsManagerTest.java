package com.zenobase.tasks.netatmo;

import static com.zenobase.tasks.RecordingTransport.bodyParams;
import static com.zenobase.tasks.RecordingTransport.queryParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.OAuth2CredentialsManagerTestSupport;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

public class NetatmoCredentialsManagerTest extends OAuth2CredentialsManagerTestSupport {

	private final NetatmoCredentialsManager manager = new NetatmoCredentialsManager(
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
		return "https://api.netatmo.com/oauth2/token";
	}

	@Override
	protected String expectedCallback(OAuthCredentials credentials) {
		return perCredentialsCallback(credentials);
	}

	@Test
	public void reauthorizeExchangesRefreshToken() {
		OAuthRequest request = refreshExpiredToken();

		assertThat(bodyParams(request)).containsEntry("client_id", "client").containsEntry("client_secret", "secret");
	}

	@Test
	public void reauthorizeThrowsInvalidTokenOnlyWhenRejected() {
		assertRefreshFailures();
	}

	@Test
	public void sendPassesTokenAsQueryParameter() {
		OAuthRequest request = sendWithToken();

		assertThat(queryParams(request)).containsEntry("access_token", "t");
		assertThat(request.getHeaders()).doesNotContainKey("Authorization");
	}
}
