package com.zenobase.tasks.dropbox;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class DropboxApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.dropbox.com/oauth2/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://www.dropbox.com/oauth2/authorize")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.build();
	}
}
