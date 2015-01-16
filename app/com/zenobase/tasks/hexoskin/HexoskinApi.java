package com.zenobase.tasks.hexoskin;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

public class HexoskinApi extends DefaultApi10a {

	@Override
	public String getRequestTokenEndpoint() {
		return "https://api.hexoskin.com/oauth/request_token";
	}

	@Override
	public String getAuthorizationUrl(Token requestToken) {
		return "https://api.hexoskin.com/oauth/authorize?oauth_token=" + requestToken.getToken();
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.hexoskin.com/oauth/access_token";
	}
}
