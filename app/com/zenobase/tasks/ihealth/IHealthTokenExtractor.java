package com.zenobase.tasks.ihealth;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.Token;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;

class IHealthTokenExtractor implements AccessTokenExtractor {

	@Override
	public Token extract(String response) {
		ObjectNode node = Nodes.readObject(response);
		String token = node.path("AccessToken").textValue();
		String refreshToken = node.path("RefreshToken").textValue();
		DateTime expires = DateTime.now(DateTimeZone.UTC).plusSeconds(node.path("Expires").intValue());
		String userId = node.path("UserID").textValue();
		return new IHealthToken(token, "", expires, refreshToken, userId);
	}
}
