package com.zenobase.tasks;

import org.joda.time.DateTime;
import org.scribe.model.Token;

public class FoursquareTask extends OAuthTask {

	private DateTime marker;

	public FoursquareTask() {
		setToken(Token.empty());
	}

	public FoursquareTask(String id, Token accessToken, DateTime marker) {
		super(id, accessToken);
		this.marker = marker;
	}

	public DateTime getMarker() {
		return marker;
	}

	public void setMarker(DateTime marker) {
		this.marker = marker;
	}
}
