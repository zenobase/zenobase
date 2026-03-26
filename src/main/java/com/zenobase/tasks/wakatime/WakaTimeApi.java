package com.zenobase.tasks.wakatime;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class WakaTimeApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://wakatime.com/oauth/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://wakatime.com/oauth/authorize")
				.addParameter("response_type", "code")
				.addParameter("client_id", config.getApiKey())
				.addParameter("redirect_uri", config.getCallback())
				.addParameter("scope", "read_logged_time,read_stats")
				.build();
	}
}
