package com.zenobase.tasks.fitbark;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

/**
 * OAuth API for FitbitBark.
 *
 * @see <a href="https://www.fitbark.com/dev/">FitBark API</a>
 */
public class FitBarkApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://app.fitbark.com/oauth/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://app.fitbark.com/oauth/authorize")
				.addParameter("response_type", "code")
				.addParameter("client_id", config.getApiKey())
				.addParameter("redirect_uri", config.getCallback())
				.build();
	}
}
