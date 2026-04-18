package com.zenobase.tasks.oura;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;
import org.scribe.model.OAuthConfig;

/**
 * OAuth API for the Oura Ring.
 *
 * @see <a href="https://cloud.ouraring.com/docs/">Oura API</a>
 */
public class OuraApi extends CustomApi20 {

	protected static final String ACCESS_TOKEN_ENDPOINT = "https://api.ouraring.com/oauth/token";

	@Override
	public String getAccessTokenEndpoint() {
		return ACCESS_TOKEN_ENDPOINT;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://cloud.ouraring.com/oauth/authorize")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.addParameter("scope", "daily")
			.build();
	}
}
