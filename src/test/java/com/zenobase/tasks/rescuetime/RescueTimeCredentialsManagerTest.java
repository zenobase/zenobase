package com.zenobase.tasks.rescuetime;

import static com.zenobase.tasks.RecordingTransport.queryParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.OAuth2CredentialsManagerTestSupport;
import com.zenobase.tasks.OAuthCredentialsManager;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

public class RescueTimeCredentialsManagerTest extends OAuth2CredentialsManagerTestSupport {

	private final RescueTimeCredentialsManager manager = new RescueTimeCredentialsManager(
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
		return "https://www.rescuetime.com/oauth/token";
	}

	@Test
	public void sendPassesTokenAsQueryParameter() {
		OAuthRequest request = sendWithToken();

		assertThat(queryParams(request)).containsEntry("access_token", "t");
		assertThat(request.getHeaders()).doesNotContainKey("Authorization");
	}
}
