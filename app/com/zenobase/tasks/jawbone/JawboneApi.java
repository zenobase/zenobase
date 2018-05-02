package com.zenobase.tasks.jawbone;

import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class JawboneApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://jawbone.com/auth/oauth2/token";
	}

	@Override
	public Verb getAccessTokenVerb() {
		return Verb.GET;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://jawbone.com/auth/oauth2/auth")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.addParameter("scope", "basic_read location_read mood_read move_read sleep_read meal_read weight_read generic_event_read")
			.build();
	}
}
