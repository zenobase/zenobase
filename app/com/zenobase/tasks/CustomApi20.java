package com.zenobase.tasks;

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

import com.zenobase.oauth.OAuth2TokenExtractor;

public abstract class CustomApi20 extends DefaultApi20 {

	@Override
	public Verb getAccessTokenVerb() {
		return Verb.POST;
	}

	@Override
	public AccessTokenExtractor getAccessTokenExtractor() {
		return new OAuth2TokenExtractor();
	}

	@Override
	public OAuthService createService(final OAuthConfig config) {
		return new OAuth20ServiceImpl(this, config) {
			@Override
			public Token getAccessToken(Token requestToken, Verifier verifier) {
				OAuthRequest request = new OAuthRequest(getAccessTokenVerb(), getAccessTokenEndpoint());
				addParameter(request, "grant_type", "authorization_code");
				addParameter(request, OAuthConstants.CLIENT_ID, config.getApiKey());
				addParameter(request, OAuthConstants.CLIENT_SECRET, config.getApiSecret());
				addParameter(request, OAuthConstants.CODE, verifier.getValue());
				addParameter(request, OAuthConstants.REDIRECT_URI, config.getCallback());
				if (config.hasScope()) {
					addParameter(request, OAuthConstants.SCOPE, config.getScope());
				}
				Response response = send(request, config);
				return getAccessTokenExtractor().extract(response.getBody());
			}
		};
	}

	private void addParameter(OAuthRequest request, String key, String value) {
		if (getAccessTokenVerb() == Verb.POST) {
			request.addBodyParameter(key, value);
		} else if (getAccessTokenVerb() == Verb.GET) {
			request.addQuerystringParameter(key, value);
		} else {
			throw new IllegalArgumentException("Can't handle: " + getAccessTokenVerb());
		}
	}

	protected Response send(OAuthRequest request, OAuthConfig config) {
		return request.send();
	}
}
