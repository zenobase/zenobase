package com.zenobase.tasks.trakt;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;
import org.scribe.model.OAuthConfig;

public class TraktApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.trakt.tv/oauth/token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		throw new UnsupportedOperationException("Trakt requires a PKCE code challenge");
	}

	public static String getAuthorizationUrl(String clientId, String callback, String codeChallenge) {
		return new UriBuilder("https://auth.trakt.tv/oauth/authorize")
			.addParameter("response_type", "code")
			.addParameter("client_id", clientId)
			.addParameter("redirect_uri", callback)
			.addParameter("code_challenge", codeChallenge)
			.addParameter("code_challenge_method", "S256")
			.build();
	}
}
