package com.zenobase.tasks.mapmyfitness;

import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class MapMyFitnessApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://oauth2-api.mapmyapi.com/v7.0/oauth2/access_token/";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://www.mapmyfitness.com/v7.0/oauth2/authorize/")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.build();
	}

	@Override
	protected Response send(OAuthRequest request, OAuthConfig config) {
		request.addHeader("Api-Key", config.getApiKey());
		return super.send(request, config);
	}
}
