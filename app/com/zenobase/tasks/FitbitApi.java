package com.zenobase.tasks;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

/**
 * OAuth API for Fitbit.
 *
 * @see <a href="https://wiki.fitbit.com/display/API">Fitbit API</a>
 */

public class FitbitApi extends DefaultApi10a {

	@Override
	public String getRequestTokenEndpoint() {
		return "https://api.fitbit.com/oauth/request_token";
	}

	@Override
	public String getAuthorizationUrl(Token requestToken) {
		return "https://www.fitbit.com/oauth/authorize?oauth_token=" + requestToken.getToken();
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.fitbit.com/oauth/access_token";
	}
}
