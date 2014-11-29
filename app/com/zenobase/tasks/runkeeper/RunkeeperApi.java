package com.zenobase.tasks.runkeeper;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class RunkeeperApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://runkeeper.com/apps/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://runkeeper.com/apps/authorize")
		.addParameter("response_type", "code")
		.addParameter("client_id", config.getApiKey())
		.addParameter("redirect_uri", config.getCallback())
		.build();
	}
}
