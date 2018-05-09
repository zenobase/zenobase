package com.zenobase.tasks.beddit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.Token;

import com.zenobase.common.UriBuilder;
import com.zenobase.json.Nodes;
import com.zenobase.tasks.CustomApi20;

public class BedditApi extends CustomApi20 {

	@Override
	public String getAccessTokenEndpoint() {
		return "https://cloudapi.beddit.com/api/v1/auth/authorize";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) {
		return new UriBuilder("https://cloudapi.beddit.com/api/v1/auth/authorize_web")
			.addParameter("response_type", "code")
			.addParameter("client_id", config.getApiKey())
			.addParameter("redirect_uri", config.getCallback())
			.build();
	}

	@Override
	public AccessTokenExtractor getAccessTokenExtractor() {
		return response -> {
			ObjectNode node = Nodes.readObject(response);
			String token = node.path(OAuthConstants.ACCESS_TOKEN).textValue();
			int userId = node.path("user").intValue();
			return new BedditToken(token, userId);
		};
	}
}
