package com.zenobase.tasks.lastfm;

import static com.zenobase.tasks.OAuth2CredentialsManagerTestSupport.apply;
import static com.zenobase.tasks.RecordingTransport.queryParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.zenobase.commands.Command;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.RecordingTransport;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

public class LastFmCredentialsManagerTest {

	private final RecordingTransport transport = new RecordingTransport();
	private final LastFmCredentialsManager manager = new LastFmCredentialsManager(
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
	public void authorizeExchangesTokenForSession() {
		OAuthCredentials credentials = manager.newCredentials(new Identity());
		transport.reply(
			200,
			"""
			{"session": {"name": "bob", "key": "session-key"}}
			"""
		);

		Command command = manager.authorize(credentials, Nodes.newObject("token", "tok"));

		assertThat(queryParams(transport.onlyRequest()))
			.containsEntry("method", "auth.getSession")
			.containsEntry("token", "tok")
			.containsEntry("api_key", "key")
			.containsEntry("format", "json")
			// md5("api_keykeymethodauth.getSessiontokentok" + "secret"); format is not signed
			.containsEntry("api_sig", "04e870be4bb79756721b7bc1937fe83d");
		var authorized = apply(command, credentials);
		assertThat(authorized.isAuthorized()).isTrue();
		assertThat(authorized.getToken().getToken()).isEqualTo("session-key");
		assertThat(authorized.getScope()).isEqualTo("bob");
	}

	@Test
	public void authorizeWithoutTokenDoesNothing() {
		OAuthCredentials credentials = manager.newCredentials(new Identity());

		assertThat(manager.authorize(credentials, Nodes.newObject())).isNull();
		assertThat(transport.requests()).isEmpty();
	}

	@Test
	public void sendSignsRequestWithSessionKey() {
		var credentials = new OAuthCredentials("lastfm", new Identity());
		credentials.setToken(new Token("sk1", ""));
		var request = new LastFmRequest();
		request.addQuerystringParameter("method", "user.getRecentTracks");
		request.addQuerystringParameter("user", "bob");
		request.addQuerystringParameter("format", "json");
		transport.reply(200, "{}");

		manager.send(request, credentials);

		assertThat(queryParams(transport.onlyRequest()))
			.containsEntry("sk", "sk1")
			.containsEntry("api_key", "key")
			// md5("api_keykeymethoduser.getRecentTrackssksk1userbob" + "secret")
			.containsEntry("api_sig", "292ac57d819d1d538d18890310866ee5");
	}
}
