package com.zenobase.tasks.lastfm;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.OAuthConfig;

public class LastFmApi extends DefaultApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return String.format("https://www.last.fm/api/auth/?api_key=%s&cb=%s", config.getApiKey(), config.getCallback());
	}
}
