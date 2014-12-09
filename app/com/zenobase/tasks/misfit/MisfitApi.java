package com.zenobase.tasks.misfit;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class MisfitApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.misfitwearables.com/auth/tokens/exchange";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://api.misfitwearables.com/auth/dialog/authorize")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.addParameter("scope", "public,birthday,email")
			.build();
	}
}
