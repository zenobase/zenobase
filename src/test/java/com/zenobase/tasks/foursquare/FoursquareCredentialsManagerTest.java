package com.zenobase.tasks.foursquare;

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

public class FoursquareCredentialsManagerTest extends OAuth2CredentialsManagerTestSupport {

	private final FoursquareCredentialsManager manager = new FoursquareCredentialsManager(
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
		return "https://foursquare.com/oauth2/access_token?grant_type=authorization_code";
	}

	@Override
	protected String expectedCallback(OAuthCredentials credentials) {
		return perCredentialsCallback(credentials);
	}

	@Test
	public void sendPassesTokenAsOAuthTokenQueryParameter() {
		OAuthRequest request = sendWithToken();

		assertThat(queryParams(request)).containsEntry("oauth_token", "t").doesNotContainKey("access_token");
		assertThat(request.getHeaders()).doesNotContainKey("Authorization");
	}
}
