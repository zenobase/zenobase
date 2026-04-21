package com.zenobase.tasks.googlehealth;

import com.google.common.base.Joiner;
import com.zenobase.oauth.OAuth2TokenExtractor;
import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuth20ServiceImpl;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;

/**
 * Google Health API OAuth2.0. The Google Health API ({@code https://health.googleapis.com}) replaces the deprecated
 * Google Fit and Fitbit Web APIs. Scopes follow the pattern {@code https://www.googleapis.com/auth/googlehealth.*}.
 *
 * @see <a href="https://developers.google.com/health/migration">Google Health migration guide</a>
 */
public class GoogleHealthApi extends DefaultApi20 {

	/**
	 * Read-only scopes covering the data types we currently map to tasks. The Google Health API defines additional
	 * scopes (e.g. {@code .location}, {@code .settings}) — add those here when we introduce tasks that need them.
	 */
	private static final String SCOPE = Joiner.on(' ').join(
		"https://www.googleapis.com/auth/userinfo.email",
		"https://www.googleapis.com/auth/googlehealth.activity_and_fitness.readonly",
		"https://www.googleapis.com/auth/googlehealth.health_metrics_and_measurements.readonly",
		"https://www.googleapis.com/auth/googlehealth.sleep.readonly",
		"https://www.googleapis.com/auth/googlehealth.nutrition.readonly",
		"https://www.googleapis.com/auth/googlehealth.profile.readonly"
	);

	private static final String AUTHORIZE_URL =
		"https://accounts.google.com/o/oauth2/v2/auth?" +
		Joiner.on('&').join(
			"response_type=code",
			"access_type=offline",
			"prompt=consent",
			"client_id=%s",
			"redirect_uri=%s",
			"scope=%s"
		);

	@Override
	public String getAccessTokenEndpoint() {
		return "https://oauth2.googleapis.com/token";
	}

	@Override
	public AccessTokenExtractor getAccessTokenExtractor() {
		return new OAuth2TokenExtractor();
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return String.format(
			AUTHORIZE_URL,
			config.getApiKey(),
			OAuthEncoder.encode(config.getCallback()),
			OAuthEncoder.encode(SCOPE)
		);
	}

	@Override
	public Verb getAccessTokenVerb() {
		return Verb.POST;
	}

	@Override
	public OAuthService createService(OAuthConfig config) {
		return new GoogleHealthOAuth2Service(this, config);
	}

	private static class GoogleHealthOAuth2Service extends OAuth20ServiceImpl {

		private static final String GRANT_TYPE = "grant_type";
		private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

		private final DefaultApi20 api;
		private final OAuthConfig config;

		public GoogleHealthOAuth2Service(DefaultApi20 api, OAuthConfig config) {
			super(api, config);
			this.api = api;
			this.config = config;
		}

		@Override
		public Token getAccessToken(Token requestToken, Verifier verifier) {
			OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());
			request.addBodyParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
			request.addBodyParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
			request.addBodyParameter(OAuthConstants.CODE, verifier.getValue());
			request.addBodyParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
			request.addBodyParameter(GRANT_TYPE, GRANT_TYPE_AUTHORIZATION_CODE);
			Response response = request.send();
			return api.getAccessTokenExtractor().extract(response.getBody());
		}
	}
}
