package com.zenobase.tasks.fitbit;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

/**
 * OAuth API for Fitbit.
 *
 * @see <a href="https://wiki.fitbit.com/display/API">Fitbit API</a>
 */

public class FitbitApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.fitbit.com/oauth2/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://www.fitbit.com/oauth2/authorize")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.addParameter("scope", "activity heartrate location nutrition sleep weight")
			.build();
	}

	@Override
	protected boolean useBasicAuthHeader() {
		return true;
	}
}
