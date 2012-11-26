package com.zenobase.tasks;

import org.scribe.model.Token;

import com.zenobase.json.TokenField;

public class FitbitTask extends OAuthTask {

	private static final TokenField MARKER = new TokenField("marker");

	public FitbitTask() {

	}

	public FitbitTask(String id, Token accessToken, String marker) {
		super(id, accessToken);
		setMarker(marker);
	}

	public String getMarker() {
		return getValue(MARKER);
	}

	public void setMarker(String marker) {
		setValue(MARKER, marker);
	}
}
