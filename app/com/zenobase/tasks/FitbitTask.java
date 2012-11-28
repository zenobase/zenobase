package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.Nodes;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class FitbitTask extends OAuthTask {

	public static final String TYPE = "fitbit";

	private static final TokenField MARKER = new TokenField("marker");

	public FitbitTask(ObjectNode node) {
		super(node);
	}

	public FitbitTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	public FitbitTask(String id, String bucketId, Identity principal, Token accessToken, String marker) {
		super(id, TYPE, bucketId, principal, accessToken);
		setMarker(marker);
	}

	public String getMarker() {
		return getValue(MARKER);
	}

	public void setMarker(String marker) {
		setValue(MARKER, marker);
	}

	@Override
	public FitbitTask copy() {
		return new FitbitTask(Nodes.copy(toJson()));
	}
}
