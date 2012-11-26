package com.zenobase.tasks;

import org.joda.time.DateTime;
import org.scribe.model.Token;

import com.zenobase.json.DateTimeField;

public class FoursquareTask extends OAuthTask {

	private static final DateTimeField MARKER = new DateTimeField("marker");

	public FoursquareTask() {
		setToken(Token.empty());
	}

	public FoursquareTask(String id, Token accessToken, DateTime marker) {
		super(id, accessToken);
		setMarker(marker);
	}

	public DateTime getMarker() {
		return getValue(MARKER);
	}

	public void setMarker(DateTime marker) {
		setValue(MARKER, marker);
	}
}
