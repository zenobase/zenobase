package com.zenobase.tasks;

import static com.zenobase.tasks.OAuth2CredentialsManagerTestSupport.TOKEN_RESPONSE;
import static com.zenobase.tasks.RecordingTransport.bodyParams;
import static com.zenobase.tasks.RecordingTransport.queryParams;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthRequest;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public class CustomApi20Test {

	private static final String CALLBACK = "https://api.example.com/oauth/callback/-";

	private final RecordingTransport transport = new RecordingTransport().reply(200, TOKEN_RESPONSE);

	@Test
	public void accessTokenRequestPostsCodeAndClientCredentials() {
		Token token = getAccessToken(new TestApi(false, false), null);

		OAuthRequest request = transport.onlyRequest();
		assertThat(request.getVerb()).isEqualTo(Verb.POST);
		assertThat(request.getUrl()).isEqualTo("https://provider.example.com/token");
		assertThat(request.getHeaders()).containsEntry("Accept", "application/json").doesNotContainKey("Authorization");
		assertThat(bodyParams(request))
			.containsEntry("grant_type", "authorization_code")
			.containsEntry("code", "the-code")
			.containsEntry("redirect_uri", CALLBACK)
			.containsEntry("client_id", "client")
			.containsEntry("client_secret", "secret")
			.doesNotContainKey("scope");
		assertThat(token.getToken()).isEqualTo("new-access");
	}

	@Test
	public void basicAuthHeaderReplacesClientSecretParameter() {
		getAccessToken(new TestApi(true, false), null);

		OAuthRequest request = transport.onlyRequest();
		assertThat(request.getHeaders()).containsEntry("Authorization", "Basic Y2xpZW50OnNlY3JldA==");
		assertThat(bodyParams(request)).containsEntry("client_id", "client").doesNotContainKey("client_secret");
	}

	@Test
	public void accessTokenRequestIncludesConfiguredScope() {
		getAccessToken(new TestApi(false, false), "read write");

		assertThat(bodyParams(transport.onlyRequest())).containsEntry("scope", "read write");
	}

	@Test
	public void signRequestPassesTokenInHeaderOrQueryString() {
		var header = new OAuthRequest(Verb.GET, "https://provider.example.com/data");
		service(new TestApi(false, true), null).signRequest(new Token("t", ""), header);
		assertThat(header.getHeaders()).containsEntry("Authorization", "Bearer t");
		assertThat(queryParams(header)).doesNotContainKey("access_token");

		var query = new OAuthRequest(Verb.GET, "https://provider.example.com/data");
		service(new TestApi(false, false), null).signRequest(new Token("t", ""), query);
		assertThat(query.getHeaders()).doesNotContainKey("Authorization");
		assertThat(queryParams(query)).containsEntry("access_token", "t");
	}

	private Token getAccessToken(CustomApi20 api, String scope) {
		return service(api, scope).getAccessToken(Token.empty(), new Verifier("the-code"));
	}

	private OAuthService service(CustomApi20 api, String scope) {
		var config = new OAuthConfig("client", "secret", CALLBACK, SignatureType.Header, scope, null);
		return api.createService(config, transport::execute);
	}

	private static class TestApi extends CustomApi20 {

		private final boolean useBasicAuthHeader;
		private final boolean passTokenInHeader;

		TestApi(boolean useBasicAuthHeader, boolean passTokenInHeader) {
			this.useBasicAuthHeader = useBasicAuthHeader;
			this.passTokenInHeader = passTokenInHeader;
		}

		@Override
		public String getAccessTokenEndpoint() {
			return "https://provider.example.com/token";
		}

		@Override
		public String getAuthorizationUrl(OAuthConfig config) {
			return "https://provider.example.com/authorize";
		}

		@Override
		protected boolean useBasicAuthHeader() {
			return useBasicAuthHeader;
		}

		@Override
		protected boolean passTokenInHeader() {
			return passTokenInHeader;
		}
	}
}
