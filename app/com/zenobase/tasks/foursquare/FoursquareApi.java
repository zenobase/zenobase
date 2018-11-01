package com.zenobase.tasks.foursquare;

import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class FoursquareApi extends CustomApi20 {

	@Override
	public Verb getAccessTokenVerb() {
		return Verb.GET;
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "https://foursquare.com/oauth2/access_token?grant_type=authorization_code";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://foursquare.com/oauth2/authenticate")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.build();
	}
}
