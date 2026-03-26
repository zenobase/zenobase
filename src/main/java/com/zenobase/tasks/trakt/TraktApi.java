package com.zenobase.tasks.trakt;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class TraktApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.trakt.tv/oauth/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://trakt.tv/oauth/authorize")
				.addParameter("response_type", "code")
				.addParameter("client_id", config.getApiKey())
				.addParameter("redirect_uri", config.getCallback())
				.build();
	}
}
