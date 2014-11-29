package com.zenobase.tasks.automatic;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class AutomaticApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://www.automatic.com/oauth/access_token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://www.automatic.com/oauth/authorize/")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("scope", "scope:vehicle scope:location scope:trip:summary")
			.build();
	}
}
