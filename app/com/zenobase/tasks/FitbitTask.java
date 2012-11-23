package com.zenobase.tasks;

import org.scribe.model.Token;

public class FitbitTask extends OAuthTask {

	private String marker;

	public FitbitTask() {

	}

	public FitbitTask(String id, Token accessToken, String marker) {
		super(id, accessToken);
		this.marker = marker;
	}

	public String getMarker() {
		return marker;
	}

	public void setMarker(String marker) {
		this.marker = marker;
	}
}
