package com.zenobase.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConstants;

import com.zenobase.json.Nodes;

public class OAuth2TokenExtractor implements AccessTokenExtractor {

	@Override
	public ExpiringToken extract(String response) {
		return extract(Nodes.readObject(response));
	}

	protected ExpiringToken extract(JsonNode node) {
		String token = node.path(OAuthConstants.ACCESS_TOKEN).textValue();
		Preconditions.checkArgument(token != null, "Can't find access_token in <%s>", node);
		String refreshToken = node.path("refresh_token").textValue();
		DateTime expires = getDateTime(node.path("expires_in"));
		return new ExpiringToken(token, "", expires, refreshToken);
	}

	private @Nullable DateTime getDateTime(JsonNode node) {
		return node.isNumber() ? DateTime.now(DateTimeZone.UTC).plusSeconds(node.intValue()) : null;
	}
}
