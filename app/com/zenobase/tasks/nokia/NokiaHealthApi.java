package com.zenobase.tasks.nokia;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

/**
 * OAuth API for Nokia Health (Withings).
 *
 * @see <a href="https://developer.health.nokia.com/api/doc">Nokia Health API</a>
 */

public class NokiaHealthApi extends CustomApi20 {

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://account.health.nokia.com/oauth2_user/authorize2")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.addParameter("scope", "user.info,user.metrics,user.activity")
			.addParameter("state", "enlightened")
			.build();
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "https://account.health.nokia.com/oauth2/token";
	}
}
