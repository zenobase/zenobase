package com.zenobase.tasks.trackthisforme;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class TrackthisformeApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://www.trackthisfor.me/oauth/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://www.trackthisfor.me/oauth/authorize")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.addParameter("scope", "read")
			.build();
	}
}
