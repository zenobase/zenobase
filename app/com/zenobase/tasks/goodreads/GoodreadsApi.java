package com.zenobase.tasks.goodreads;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import com.zenobase.common.UriBuilder;

public class GoodreadsApi extends DefaultApi10a {

	private final String callbackUrl;

	public GoodreadsApi(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}

	@Override
	public String getRequestTokenEndpoint() {
		return "https://www.goodreads.com/oauth/request_token";
	}

	@Override
	public Verb getRequestTokenVerb() {
		return Verb.GET;
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "https://www.goodreads.com/oauth/access_token";
	}

	@Override
	public String getAuthorizationUrl(Token requestToken) {
		return new UriBuilder("https://www.goodreads.com/oauth/authorize")
			.addParameter("oauth_token", requestToken.getToken())
			.addParameter("oauth_callback", callbackUrl)
			.build();
	}
}
