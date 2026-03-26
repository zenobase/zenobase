package com.zenobase.tasks.withings;

import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthRequest;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

/**
 * OAuth API for Withings (formerly Nokia Health, formerly Withings).
 *
 * @see <a href="http://developer.withings.com/">Withings API</a>
 */
public class WithingsApi extends CustomApi20 {

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://account.withings.com/oauth2_user/authorize2")
				.addParameter("response_type", "code")
				.addParameter("client_id", config.getApiKey())
				.addParameter("redirect_uri", config.getCallback())
				.addParameter("scope", "user.info,user.metrics,user.activity")
				.addParameter("state", "enlightened")
				.build();
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "https://wbsapi.withings.net/v2/oauth2";
	}

	@Override
	public AccessTokenExtractor getAccessTokenExtractor() {
		return new WithingsAccessTokenExtractor();
	}

	@Override
	protected void configureAccessTokenRequest(OAuthRequest request) {
		addParameter(request, "action", "requesttoken");
	}
}
