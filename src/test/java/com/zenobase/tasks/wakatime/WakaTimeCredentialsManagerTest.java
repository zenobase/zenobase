package com.zenobase.tasks.wakatime;

import static com.zenobase.tasks.RecordingTransport.bodyParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.OAuth2CredentialsManagerTestSupport;
import com.zenobase.tasks.OAuthCredentialsManager;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

public class WakaTimeCredentialsManagerTest extends OAuth2CredentialsManagerTestSupport {

	private final WakaTimeCredentialsManager manager = new WakaTimeCredentialsManager(
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
		return "https://wakatime.com/oauth/token";
	}

	@Test
	public void reauthorizeExchangesRefreshToken() {
		OAuthRequest request = refreshExpiredToken();

		assertThat(request.getHeaders()).containsEntry("Accept", "application/json");
		assertThat(bodyParams(request)).containsEntry("client_id", "client").containsEntry("client_secret", "secret");
	}

	@Test
	public void reauthorizeThrowsInvalidTokenOnlyWhenRejected() {
		assertRefreshFailures();
	}

	@Test
	public void sendPassesTokenInBearerHeader() {
		assertThat(sendWithToken().getHeaders()).containsEntry("Authorization", "Bearer t");
	}
}
