package com.zenobase.tasks.ihealth;

import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;

import com.zenobase.common.UriBuilder;
import com.zenobase.tasks.CustomApi20;

public class IHealthApi extends CustomApi20 {

	static final String ENDPOINT = "https://api.ihealthlabs.com:8443/OpenApiV2/OAuthv2/userauthorization/";

	@Override
	public String getAccessTokenEndpoint() {
		return ENDPOINT;
	}

	@Override
	public Verb getAccessTokenVerb() {
		return Verb.GET;
	}

	@Override
	public AccessTokenExtractor getAccessTokenExtractor() {
		return new IHealthTokenExtractor();
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder(ENDPOINT)
				.addParameter("response_type", "code")
				.addParameter("client_id", config.getApiKey())
				.addParameter("redirect_uri", config.getCallback())
				.addParameter(
						"APIName",
						"OpenApiActivity OpenApiBP OpenApiSleep OpenApiWeight OpenApiBG OpenApiSpO2 OpenApiUserInfo OpenApiFood OpenApiSport")
				.build();
	}
}
