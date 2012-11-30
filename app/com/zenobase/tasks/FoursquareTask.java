package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.scribe.model.Token;

import com.zenobase.json.DateTimeField;
import com.zenobase.models.Identity;

public class FoursquareTask extends OAuthTask {

	public static final String TYPE = "foursquare";

	public static final DateTimeField MARKER = new DateTimeField("marker");

	public FoursquareTask(ObjectNode node) {
		super(node);
	}

	public FoursquareTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
		setConfigValue(TOKEN, Token.empty());
	}

	public FoursquareTask(String id, Task.State state, String bucketId, Identity principal, Token accessToken, DateTime marker) {
		super(id, TYPE, state, bucketId, principal, accessToken);
		setConfigValue(MARKER, marker);
	}

	public DateTime getMarker() {
		return getConfigValue(MARKER);
	}

	@Override
	public FoursquareTask copy() {
		return copy(getClass());
	}
}
