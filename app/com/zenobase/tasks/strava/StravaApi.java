package com.zenobase.tasks.strava;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class StravaApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://www.strava.com/oauth/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://www.strava.com/oauth/authorize")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.build();
	}
}
