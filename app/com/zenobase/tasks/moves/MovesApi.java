package com.zenobase.tasks.moves;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class MovesApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.moves-app.com/oauth/v1/access_token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://api.moves-app.com/oauth/v1/authorize")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.addParameter("scope", "activity location")
			.build();
	}
}
