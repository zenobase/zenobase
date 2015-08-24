package com.zenobase.tasks.microsoft;

import org.scribe.model.OAuthConfig;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class MicrosoftHealthApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://login.live.com/oauth20_token.srf";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://login.live.com/oauth20_authorize.srf")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.addParameter("scope", "mshealth.ReadActivityHistory mshealth.ReadDevices mshealth.ReadActivityLocation offline_access")
			.build();
	}
}
