package com.zenobase.tasks.jawbone;

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
import org.scribe.utils.Preconditions;

import com.zenobase.oauth.OAuth2TokenExtractor;

public class JawboneApi extends DefaultApi20 {

	private static final String SCOPE_READ_ONLY = "basic_read location_read mood_read move_read sleep_read meal_read weight_read generic_event_read";
	private static final String AUTHORIZATION_URL = "https://jawbone.com/auth/oauth2/auth?response_type=code&client_id=%s&redirect_uri=%s&scope=%s";

	@Override
	public String getAccessTokenEndpoint() {
		return "https://jawbone.com/auth/oauth2/token";
	}

	@Override
	public Verb getAccessTokenVerb() {
		return Verb.GET;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		Preconditions.checkValidUrl(config.getCallback(), "Must provide a valid url as callback.");
		return String.format(AUTHORIZATION_URL, config.getApiKey(), OAuthEncoder.encode(config.getCallback()), SCOPE_READ_ONLY);
	}

	@Override
	public AccessTokenExtractor getAccessTokenExtractor() {
		return new OAuth2TokenExtractor();
	}

	// workaround for https://github.com/fernandezpablo85/scribe-java/issues/368
	@Override
	public OAuthService createService(final OAuthConfig config) {
		return new OAuth20ServiceImpl(this, config) {
			@Override
			public Token getAccessToken(Token requestToken, Verifier verifier) {
				OAuthRequest request = new OAuthRequest(getAccessTokenVerb(), getAccessTokenEndpoint());
				request.addQuerystringParameter("grant_type", "authorization_code");
				request.addQuerystringParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
				request.addQuerystringParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
				request.addQuerystringParameter(OAuthConstants.CODE, verifier.getValue());
				Response response = request.send();
				return getAccessTokenExtractor().extract(response.getBody());
			}
		};
	}
}
