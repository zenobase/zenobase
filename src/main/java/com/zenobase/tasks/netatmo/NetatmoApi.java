package com.zenobase.tasks.netatmo;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class NetatmoApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.netatmo.com/oauth2/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://api.netatmo.com/oauth2/authorize")
				.addParameter("client_id", config.getApiKey())
				.addParameter("redirect_uri", config.getCallback())
				.addParameter("scope", "read_station")
				.build();
	}
}
