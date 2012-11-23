package com.zenobase.tasks;

import org.scribe.model.Token;

public class WithingsTask extends OAuthTask {

	private int userId;
	private String marker;

	public WithingsTask() {

	}

	public WithingsTask(String id, Token token, int userId, String marker) {
		super(id, token);
		this.userId = userId;
		this.marker = marker;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getMarker() {
		return marker;
	}

	public void setMarker(String marker) {
		this.marker = marker;
	}
}