package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.LocalDate;
import org.scribe.model.Token;

import com.zenobase.json.LocalDateField;
import com.zenobase.models.Identity;

public class FitbitTask extends OAuthTask {

	public static final String TYPE = "fitbit";
	public static final LocalDateField MARKER = new LocalDateField("marker");

	public FitbitTask(ObjectNode node) {
		super(node);
	}

	public FitbitTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	public FitbitTask(String id, Task.State state, String bucketId, Identity principal, Token accessToken, LocalDate marker) {
		super(id, TYPE, state, bucketId, principal, accessToken);
		setConfigValue(MARKER, marker);
	}

	public LocalDate getMarker() {
		return getConfigValue(MARKER);
	}

	@Override
	public FitbitTask copy() {
		return copy(getClass());
	}
}
