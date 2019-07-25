package com.zenobase.tasks.garmin;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

import com.zenobase.common.UriBuilder;

public class GarminApi extends DefaultApi10a {

	private final String callbackUrl;

	public GarminApi(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}

	@Override
	public String getRequestTokenEndpoint() {
		return "https://connectapi.garmin.com/oauth-service/oauth/request_token";
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "https://connectapi.garmin.com/oauth-service/oauth/access_token";
	}

	@Override
	public String getAuthorizationUrl(Token requestToken) {
		return new UriBuilder("https://connect.garmin.com/oauthConfirm")
			.addParameter("oauth_token", requestToken.getToken())
			.addParameter("oauth_callback", callbackUrl)
			.build();
	}
}
