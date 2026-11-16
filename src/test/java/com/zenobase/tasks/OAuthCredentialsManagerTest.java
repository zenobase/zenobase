package com.zenobase.tasks;

import static com.zenobase.tasks.OAuth2CredentialsManagerTestSupport.TOKEN_RESPONSE;
import static com.zenobase.tasks.OAuth2CredentialsManagerTestSupport.apply;
import static com.zenobase.tasks.OAuth2CredentialsManagerTestSupport.assertNewToken;
import static com.zenobase.tasks.RecordingTransport.bodyParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.repositories.CredentialsRepository;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

/**
 * Covers the base class's request-token flow: checking the {@code oauth_token} from the callback and exchanging its
 * {@code oauth_verifier}. A stub OAuth 2 provider stands in for Scribe's OAuth 1 exchange, which can't be intercepted.
 */
public class OAuthCredentialsManagerTest {

	private final RecordingTransport transport = new RecordingTransport();
	private int requestTokens = 0;

	private final OAuthCredentialsManager manager = new OAuthCredentialsManager(
		"test",
		mock(CredentialsRepository.class),
		new CustomApi20() {
			@Override
			public String getAccessTokenEndpoint() {
				return "https://provider.example.com/token";
			}

			@Override
			public String getAuthorizationUrl(OAuthConfig config) {
				return "https://provider.example.com/authorize?callback=" + config.getCallback();
			}
		},
		"client",
		"secret",
		"https://api.example.com"
	) {
		@Override
		protected Token getRequestToken(OAuthCredentials credentials) {
			return new Token("request-" + ++requestTokens, "request-secret");
		}

		@Override
		protected Response execute(OAuthRequest request) {
			return transport.execute(request);
		}
	};

	@Test
	public void newCredentialsAwaitAuthorizationAtPerCredentialsCallback() {
		OAuthCredentials credentials = manager.newCredentials(new Identity());

		assertThat(credentials.isAuthorized()).isFalse();
		assertThat(credentials.getToken().getToken()).isEqualTo("request-1");
		assertThat(credentials.getAuthorizationUrl()).isEqualTo(
			"https://provider.example.com/authorize?callback=https://api.example.com/oauth/callback/" +
				credentials.getId()
		);
	}

	@Test
	public void authorizeExchangesVerifierForAccessToken() {
		OAuthCredentials credentials = manager.newCredentials(new Identity());
		transport.reply(200, TOKEN_RESPONSE);

		var command = manager.authorize(
			credentials,
			Nodes.newObject().put("oauth_token", "request-1").put("oauth_verifier", "verifier")
		);

		assertThat(bodyParams(transport.onlyRequest())).containsEntry("code", "verifier");
		OAuthCredentials authorized = apply(command, credentials);
		assertThat(authorized.isAuthorized()).isTrue();
		assertNewToken(authorized);
	}

	@Test
	public void authorizeWithoutTokenDoesNothing() {
		OAuthCredentials credentials = manager.newCredentials(new Identity());

		assertThat(manager.authorize(credentials, Nodes.newObject("denied", "request-1"))).isNull();
		assertThat(manager.authorize(credentials, Nodes.newObject())).isNull();
		assertThat(transport.requests()).isEmpty();
	}

	@Test
	public void authorizeRejectsTokenOfAnotherRequest() {
		OAuthCredentials credentials = manager.newCredentials(new Identity());
		var config = Nodes.newObject().put("oauth_token", "request-0").put("oauth_verifier", "verifier");

		assertThatThrownBy(() -> manager.authorize(credentials, config)).isInstanceOf(IllegalStateException.class);
		assertThat(transport.requests()).isEmpty();
	}

	@Test
	public void authorizeRejectsAuthorizedCredentials() {
		var credentials = new OAuthCredentials("test", new Identity());
		credentials.setToken(new Token("access", ""));
		var config = Nodes.newObject().put("oauth_token", "access").put("oauth_verifier", "verifier");

		assertThatThrownBy(() -> manager.authorize(credentials, config)).isInstanceOf(IllegalStateException.class);
		assertThat(transport.requests()).isEmpty();
	}
}
