package com.zenobase.tasks.withings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.Nodes;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.oauth.OAuth2TokenExtractor;

class WithingsAccessTokenExtractor extends OAuth2TokenExtractor {

	@Override
	public ExpiringToken extract(String response) {
		ObjectNode node = Nodes.readObject(response);
		return extract(node.path("body"));
	}
}
