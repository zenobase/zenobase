package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.LocalDate;
import org.scribe.model.Token;

import com.zenobase.json.LocalDateField;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;

public class FitbitTask extends OAuthTask {

	public static final String TYPE = "fitbit";

	private static final LocalDateField MARKER = new LocalDateField("marker");

	public FitbitTask(ObjectNode node) {
		super(node);
	}

	public FitbitTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	public FitbitTask(String id, Task.State state, String bucketId, Identity principal, Token accessToken, LocalDate marker) {
		super(id, TYPE, state, bucketId, principal, accessToken);
		setMarker(marker);
	}

	public LocalDate getMarker() {
		return getConfigValue(MARKER);
	}

	public void setMarker(LocalDate marker) {
		setConfigValue(MARKER, marker);
	}

	@Override
	public FitbitTask copy() {
		return new FitbitTask(Nodes.copy(toJson()));
	}
}
