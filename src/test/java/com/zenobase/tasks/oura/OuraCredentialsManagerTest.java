package com.zenobase.tasks.oura;

import static com.zenobase.tasks.RecordingTransport.bodyParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.OAuth2CredentialsManagerTestSupport;
import com.zenobase.tasks.OAuthCredentialsManager;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

public class OuraCredentialsManagerTest extends OAuth2CredentialsManagerTestSupport {

	private final OuraCredentialsManager manager = new OuraCredentialsManager(
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
		return "https://api.ouraring.com/oauth/token";
	}

	@Test
	public void reauthorizeExchangesRefreshToken() {
		OAuthRequest request = refreshExpiredToken();

		assertThat(bodyParams(request)).containsEntry("client_id", "client").containsEntry("client_secret", "secret");
	}

	@Test
	public void sendPassesTokenInBearerHeader() {
		assertThat(sendWithToken().getHeaders()).containsEntry("Authorization", "Bearer t");
	}
}
