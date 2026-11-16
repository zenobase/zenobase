package com.zenobase.tasks;

import static com.zenobase.tasks.RecordingTransport.bodyParams;
import static com.zenobase.tasks.RecordingTransport.queryParams;
import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.ExpiringToken;
import java.net.URI;
import java.util.Map;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;

/**
 * Covers the authorization code flow that each OAuth 2 credentials manager implements on its own: exchanging the
 * {@code code} from the callback for a token, at the same redirect URI that the authorization URL named.
 */
public abstract class OAuth2CredentialsManagerTestSupport {

	protected static final String HOST = "https://api.example.com";
	protected static final String FIXED_CALLBACK = HOST + "/oauth/callback/-";
	protected static final String TOKEN_RESPONSE = """
	{
		"access_token": "new-access",
		"refresh_token": "new-refresh",
		"expires_in": 3600
	}
	""";

	protected final RecordingTransport transport = new RecordingTransport();

	protected abstract OAuthCredentialsManager manager();

	protected abstract String tokenEndpoint();

	protected String expectedCallback(OAuthCredentials credentials) {
		return FIXED_CALLBACK;
	}

	protected String tokenResponse() {
		return TOKEN_RESPONSE;
	}

	/** Checks what the token request adds beyond the authorization code grant; by default, the client credentials. */
	protected void assertTokenRequest(OAuthRequest request, Map<String, String> params, OAuthCredentials credentials) {
		assertThat(params).containsEntry("client_id", "client").containsEntry("client_secret", "secret");
	}

	@Test
	public void authorizeExchangesCodeForToken() {
		OAuthCredentials credentials = manager().newCredentials(new Identity());
		String authorizationUrl = credentials.getAuthorizationUrl();
		transport.reply(200, tokenResponse());

		Command command = manager().authorize(credentials, Nodes.newObject("code", "the-code"));

		OAuthRequest request = transport.onlyRequest();
		assertThat(request.getUrl()).isEqualTo(tokenEndpoint());
		Map<String, String> params = tokenParams(request);
		assertThat(params)
			.containsEntry("grant_type", "authorization_code")
			.containsEntry("code", "the-code")
			.containsEntry("redirect_uri", expectedCallback(credentials));
		assertThat(queryParams(URI.create(authorizationUrl))).containsEntry(
			"redirect_uri",
			expectedCallback(credentials)
		);
		assertTokenRequest(request, params, credentials);

		OAuthCredentials authorized = apply(command, credentials);
		assertThat(authorized.isAuthorized()).isTrue();
		assertNewToken(authorized);
	}

	@Test
	public void authorizeWithoutCodeDoesNothing() {
		OAuthCredentials credentials = manager().newCredentials(new Identity());

		assertThat(manager().authorize(credentials, Nodes.newObject("error", "access_denied"))).isNull();
		assertThat(transport.requests()).isEmpty();
	}

	protected static String perCredentialsCallback(OAuthCredentials credentials) {
		return HOST + "/oauth/callback/" + credentials.getId();
	}

	/** Refreshes an expired token, checking the parts every refresh shares; returns the request for the rest. */
	protected OAuthRequest refreshExpiredToken() {
		OAuthCredentials credentials = authorizedCredentials(expiredToken());
		transport.reply(200, tokenResponse());

		manager().reauthorize(credentials);

		OAuthRequest request = transport.onlyRequest();
		assertThat(request.getVerb()).isEqualTo(Verb.POST);
		assertThat(request.getUrl()).isEqualTo(tokenEndpoint());
		assertThat(bodyParams(request))
			.containsEntry("grant_type", "refresh_token")
			.containsEntry("refresh_token", "old-refresh");
		assertNewToken(credentials);
		return request;
	}

	/** Sends an API request with access token {@code t}, returning it as signed. */
	protected OAuthRequest sendWithToken() {
		transport.reply(200, "{}");
		manager().send(
			new OAuthRequest(Verb.GET, "https://provider.example.com/data"),
			authorizedCredentials(new Token("t", ""))
		);
		return transport.onlyRequest();
	}

	protected OAuthCredentials authorizedCredentials(Token token) {
		var credentials = new OAuthCredentials(manager().getType(), new Identity());
		credentials.setToken(token);
		return credentials;
	}

	protected static ExpiringToken expiredToken() {
		return new ExpiringToken("old-access", "", DateTime.now().minusMinutes(5), "old-refresh");
	}

	protected static void assertNewToken(OAuthCredentials credentials) {
		var token = (ExpiringToken) credentials.getToken();
		assertThat(token.getToken()).isEqualTo("new-access");
		assertThat(token.getRefreshToken()).isEqualTo("new-refresh");
		assertThat(token.isExpired()).isFalse();
	}

	public static OAuthCredentials apply(@Nullable Command command, Credentials credentials) {
		assertThat(command).isInstanceOf(UpdateCredentialsCommand.class);
		return ((UpdateCredentialsCommand) command).apply(credentials).as(OAuthCredentials.class);
	}

	private static Map<String, String> tokenParams(OAuthRequest request) {
		return request.getVerb() == Verb.GET ? queryParams(request) : bodyParams(request);
	}
}
