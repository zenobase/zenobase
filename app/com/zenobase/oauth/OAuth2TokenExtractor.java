package com.zenobase.oauth;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConstants;

import com.zenobase.json.Nodes;

public class OAuth2TokenExtractor implements AccessTokenExtractor {

	@Override
	public ExpiringToken extract(String response) {
		ObjectNode node = Nodes.readObject(response);
		String token = node.path(OAuthConstants.ACCESS_TOKEN).getTextValue();
		String refreshToken = node.path("refresh_token").getTextValue();
		DateTime expires = getDateTime(node.path("expires_in"));
		return new ExpiringToken(token, "", expires, refreshToken);
	}

	private DateTime getDateTime(JsonNode node) {
		return node.isNumber() ? DateTime.now(DateTimeZone.UTC).plusSeconds(node.getIntValue()) : null;
	}
}
