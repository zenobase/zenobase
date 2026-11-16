package com.zenobase.tasks.mapmyfitness;

import static com.zenobase.tasks.RecordingTransport.bodyParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.OAuth2CredentialsManagerTestSupport;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthCredentialsManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

public class MapMyFitnessCredentialsManagerTest extends OAuth2CredentialsManagerTestSupport {

	private final MapMyFitnessCredentialsManager manager = new MapMyFitnessCredentialsManager(
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
		return "https://api.ua.com/v7.0/oauth2/uacf/access_token/";
	}

	@Override
	protected void assertTokenRequest(OAuthRequest request, Map<String, String> params, OAuthCredentials credentials) {
		super.assertTokenRequest(request, params, credentials);
		assertThat(request.getHeaders()).containsEntry("Api-Key", "client");
	}

	@Test
	public void reauthorizeExchangesRefreshToken() {
		OAuthRequest request = refreshExpiredToken();

		assertThat(request.getHeaders()).containsEntry("Api-Key", "client");
		assertThat(bodyParams(request)).containsEntry("client_id", "client").containsEntry("client_secret", "secret");
	}

	@Test
	public void reauthorizeThrowsInvalidTokenOnlyWhenRejected() {
		assertRefreshFailures();
	}

	@Test
	public void sendPassesApiKeyAndBearerHeaders() {
		assertThat(sendWithToken().getHeaders())
			.containsEntry("Api-Key", "client")
			.containsEntry("Authorization", "Bearer t");
	}
}
