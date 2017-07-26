package com.zenobase.tasks.hexoskin;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class HexoskinApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.hexoskin.com/api/connect/oauth2/token/";
	}

	@Override
	protected boolean passTokenInHeader() {
		return true;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://api.hexoskin.com/api/connect/oauth2/auth/")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.build();
	}
}
