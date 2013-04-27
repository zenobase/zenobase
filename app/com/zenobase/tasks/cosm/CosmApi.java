package com.zenobase.tasks.cosm;

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

public class CosmApi extends DefaultApi20 {

	private static final String AUTHORIZATION_URL = "https://cosm.com/oauth/authenticate?client_id=%s&redirect_uri=%s";

	@Override
	public String getAccessTokenEndpoint() {
		return "https://cosm.com/oauth/token";
	}

	@Override
	public Verb getAccessTokenVerb() {
		return Verb.POST;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		Preconditions.checkValidUrl(config.getCallback(), "Must provide a valid url as callback.");
		return String.format(AUTHORIZATION_URL, config.getApiKey(), OAuthEncoder.encode(config.getCallback()));
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
				request.addBodyParameter("grant_type", "authorization_code");
				request.addBodyParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
				request.addBodyParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
				request.addBodyParameter(OAuthConstants.CODE, verifier.getValue());
				request.addBodyParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
				if (config.hasScope()) {
					request.addBodyParameter(OAuthConstants.SCOPE, config.getScope());
				}
				Response response = request.send();
				return getAccessTokenExtractor().extract(response.getBody());
			}
		};
	}
}
