package com.zenobase.tasks.dash;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class DashApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://dash.by/api/auth/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://dash.by/api/auth/authorize")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("scope", "user trips")
			.build();
	}
}
