package com.zenobase.tasks.nokia;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;
import org.scribe.model.Verb;

/**
 * OAuth API for Nokia Health (Withings).
 *
 * @see <a href="https://developer.health.nokia.com/api/doc">Nokia Health API</a>
 */

public class NokiaHealthApi extends DefaultApi10a {

	@Override
	public String getRequestTokenEndpoint() {
		return "https://developer.health.nokia.com/account/request_token";
	}

	@Override
	public Verb getRequestTokenVerb() {
		return Verb.GET;
	}

	@Override
	public String getAuthorizationUrl(Token requestToken) {
		return "https://developer.health.nokia.com/account/authorize?oauth_token=" + requestToken.getToken();
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "https://developer.health.nokia.com/account/access_token";
	}
}
