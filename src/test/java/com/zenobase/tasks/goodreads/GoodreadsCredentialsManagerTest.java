package com.zenobase.tasks.goodreads;

import static com.zenobase.tasks.RecordingTransport.queryParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.RecordingTransport;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

public class GoodreadsCredentialsManagerTest {

	private final RecordingTransport transport = new RecordingTransport();
	private final GoodreadsCredentialsManager manager = new GoodreadsCredentialsManager(
		mock(CredentialsRepository.class),
		"key",
		"secret",
		"https://api.example.com"
	) {
		@Override
		protected Response execute(OAuthRequest request) {
			return transport.execute(request);
		}
	};

	@Test
	public void authorizeRequiresApproval() {
		var credentials = new OAuthCredentials("goodreads", new Identity());
		credentials.setToken(new Token("request", "request-secret"));
		credentials.setAuthorizationUrl("https://www.goodreads.com/oauth/authorize?oauth_token=request");

		assertThat(
			manager.authorize(credentials, Nodes.newObject().put("oauth_token", "request").put("authorize", "0"))
		).isNull();
		assertThat(manager.authorize(credentials, Nodes.newObject().put("authorize", "1"))).isNull();
	}

	@Test
	public void authorizeRejectsMismatchedToken() {
		var credentials = new OAuthCredentials("goodreads", new Identity());
		credentials.setToken(new Token("request", "request-secret"));

		assertThatThrownBy(() ->
			manager.authorize(credentials, Nodes.newObject().put("oauth_token", "other").put("authorize", "1"))
		).isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void sendSignsRequestInQueryString() {
		var credentials = new OAuthCredentials("goodreads", new Identity());
		credentials.setToken(new Token("access", "access-secret"));
		transport.reply(200, "<GoodreadsResponse/>");

		manager.send(new OAuthRequest(Verb.GET, "https://www.goodreads.com/review/list/1.xml"), credentials);

		OAuthRequest request = transport.onlyRequest();
		assertThat(queryParams(request))
			.containsEntry("oauth_token", "access")
			.containsEntry("oauth_consumer_key", "key")
			.containsEntry("oauth_signature_method", "HMAC-SHA1")
			.containsKey("oauth_signature");
		assertThat(request.getHeaders()).doesNotContainKey("Authorization");
	}
}
