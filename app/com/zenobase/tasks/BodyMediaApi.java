package com.zenobase.tasks;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.oauth.OAuth10aServiceImpl;
import org.scribe.oauth.OAuthService;

/**
 * OAuth API for BodyMedia.
 *
 * @see <a href="https://developer.bodymedia.com/docs/">BodyMedia API</a>
 */

public class BodyMediaApi extends DefaultApi10a {

	private static final String BASE = "https://api.bodymedia.com/oauth";

	private final String apiKey;

	public BodyMediaApi(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public String getRequestTokenEndpoint() {
		return String.format("%s/request_token?api_key=%s", BASE, apiKey);
	}

	@Override
	public String getAuthorizationUrl(Token requestToken) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAccessTokenEndpoint() {
		return String.format("%s/access_token?api_key=%s", BASE, apiKey);
	}

	@Override
	public OAuthService createService(final OAuthConfig config) {
	    return new OAuth10aServiceImpl(this, config) {
	    	@Override
	    	public String getAuthorizationUrl(Token requestToken) {
	    		return String.format("%s/authorize?api_key=%s&oauth_token=%s&oauth_callback=%s", BASE, apiKey, requestToken.getToken(), config.getCallback());
	    	}
	    };
	}
}
