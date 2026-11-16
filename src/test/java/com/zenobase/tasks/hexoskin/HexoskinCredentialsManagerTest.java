package com.zenobase.tasks.hexoskin;

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
import org.scribe.model.Token;

public class HexoskinCredentialsManagerTest extends OAuth2CredentialsManagerTestSupport {

	private final HexoskinCredentialsManager manager = new HexoskinCredentialsManager(
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
		return "https://api.hexoskin.com/api/connect/oauth2/token/";
	}

	@Test
	public void reauthorizeExchangesRefreshTokenWithBasicAuth() {
		OAuthRequest request = refreshExpiredToken();

		assertThat(request.getHeaders()).containsEntry("Authorization", "Basic Y2xpZW50OnNlY3JldA==");
		assertThat(bodyParams(request)).doesNotContainKey("client_secret");
	}

	@Test
	public void reauthorizeConvertsLegacyTokenIntoRefreshToken() {
		OAuthCredentials credentials = authorizedCredentials(new Token("key", "secret"));
		transport.reply(200, tokenResponse());

		manager.reauthorize(credentials);

		assertThat(bodyParams(transport.onlyRequest())).containsEntry("refresh_token", "key:secret");
		assertNewToken(credentials);
	}

	@Test
	public void reauthorizeKeepsTokenWhenRefreshFails() {
		OAuthCredentials credentials = authorizedCredentials(expiredToken());
		transport.reply(400, "{\"error\":\"invalid_grant\"}");

		manager.reauthorize(credentials);

		assertThat(credentials.getToken().getToken()).isEqualTo("old-access");
	}

	@Test
	public void sendPassesTokenInBearerHeader() {
		OAuthRequest request = sendWithToken();

		assertThat(request.getHeaders()).containsEntry("Authorization", "Bearer t");
		assertThat(queryParams(request)).doesNotContainKey("access_token");
	}
}
