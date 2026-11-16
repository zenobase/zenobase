package com.zenobase.tasks;

import static com.zenobase.tasks.OAuth2CredentialsManagerTestSupport.apply;
import static com.zenobase.tasks.RecordingTransport.response;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.Command;
import com.zenobase.models.Identity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

public class OAuthTaskManagerTest {

	private final OAuthCredentialsManager credentialsManager = mock(OAuthCredentialsManager.class);
	private final Identity principal = new Identity();
	private final Task task = new Task("test", "bucket", principal);

	private final OAuthTaskManager manager = new OAuthTaskManager("test", credentialsManager) {
		@Override
		public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
			return new Task("test", bucketId, principal);
		}

		@Override
		public @Nullable Command execute(Task task, OAuthCredentials credentials) {
			return null;
		}
	};

	@Test
	public void executeRequiresCredentials() {
		when(credentialsManager.getType()).thenReturn("test");

		assertThatThrownBy(() -> manager.execute(task)).isInstanceOfSatisfying(MissingCredentialsException.class, e ->
			assertThat(e.getExpectedType()).isEqualTo("test")
		);
	}

	@Test
	public void executeRequiresAuthorizedCredentials() {
		var credentials = new OAuthCredentials("test", principal);
		credentials.setAuthorizationUrl("https://provider.example.com/authorize");
		when(credentialsManager.find(principal)).thenReturn(credentials);

		assertThatThrownBy(() -> manager.execute(task)).isInstanceOf(IncompleteCredentialsException.class);
	}

	@Test
	public void rejectedRefreshInvalidatesToken() {
		var credentials = new OAuthCredentials("test", principal);
		doThrow(new IllegalArgumentException("Can't find access_token"))
			.when(credentialsManager)
			.reauthorize(credentials);

		assertThatThrownBy(() -> manager.reauthorize(credentials)).isInstanceOfSatisfying(
			InvalidTokenException.class,
			e -> assertThat(e.getCredentials()).isSameAs(credentials)
		);
	}

	@Test
	public void recoverInvalidTokenRestartsAuthorization() {
		var credentials = new OAuthCredentials("test", principal);
		credentials.setToken(new Token("revoked", ""));
		var requestToken = new Token("request", "verifier");
		when(credentialsManager.getRequestToken(credentials)).thenReturn(requestToken);
		when(credentialsManager.getAuthorizationUrl(credentials, requestToken)).thenReturn(
			"https://provider.example.com/authorize?state=new"
		);

		OAuthCredentials reset = apply(
			manager.recoverInvalidToken(new InvalidTokenException(credentials)),
			credentials
		);

		assertThat(reset.isAuthorized()).isFalse();
		assertThat(reset.getAuthorizationUrl()).isEqualTo("https://provider.example.com/authorize?state=new");
		assertThat(reset.getToken()).isEqualTo(requestToken);
	}

	@Test
	public void sendAcceptsGzip() {
		var credentials = new OAuthCredentials("test", principal);
		var request = new OAuthRequest(Verb.GET, "https://provider.example.com/data");
		Response ok = response(200, "{}");
		when(credentialsManager.send(request, credentials)).thenReturn(ok);

		manager.send(request, credentials);

		assertThat(request.getHeaders()).containsEntry("Accept-Encoding", "gzip");
	}

	@Test
	public void parseDecodesGzipBody() throws IOException {
		assertThat(
			OAuthTaskManager.parseObject(gzipped("Content-Encoding", "{\"a\":1}")).path("a").intValue()
		).isEqualTo(1);
		assertThat(
			OAuthTaskManager.parseObject(gzipped("content-encoding", "{\"a\":2}")).path("a").intValue()
		).isEqualTo(2);
	}

	private static Response gzipped(String header, String body) throws IOException {
		var bytes = new ByteArrayOutputStream();
		try (var out = new GZIPOutputStream(bytes)) {
			out.write(body.getBytes(StandardCharsets.UTF_8));
		}
		Response response = response(200, "not decoded");
		when(response.getHeader(header)).thenReturn("gzip");
		when(response.getStream()).thenReturn(new ByteArrayInputStream(bytes.toByteArray()));
		return response;
	}
}
