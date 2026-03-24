package com.zenobase.tasks.beeminder;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class BeeminderApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://www.beeminder.com/apps/authorize")
			.addParameter("response_type", "token")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.build();
	}
}
