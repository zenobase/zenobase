package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.scribe.model.Token;

import com.zenobase.json.DateTimeField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;

public class FoursquareTask extends OAuthTask {

	public static final String TYPE = "foursquare";

	private static final DateTimeField MARKER = new DateTimeField("marker");

	public FoursquareTask(ObjectNode node) {
		super(node);
	}

	public FoursquareTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
		setToken(Token.empty());
	}

	public FoursquareTask(String id, String bucketId, Identity principal, Token accessToken, DateTime marker) {
		super(id, TYPE, bucketId, principal, accessToken);
		setMarker(marker);
	}

	public DateTime getMarker() {
		return getValue(MARKER);
	}

	public void setMarker(DateTime marker) {
		setValue(MARKER, marker);
	}

	@Override
	public FoursquareTask copy() {
		return new FoursquareTask(Nodes.copy(toJson()));
	}
}
