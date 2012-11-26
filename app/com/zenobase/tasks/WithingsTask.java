package com.zenobase.tasks;

import org.scribe.model.Token;

import com.zenobase.json.IntegerField;
import com.zenobase.json.TokenField;

public class WithingsTask extends OAuthTask {

	private static final IntegerField USER_ID = new IntegerField("userId");
	private static final TokenField MARKER = new TokenField("marker");

	public WithingsTask() {

	}

	public WithingsTask(String id, Token token, int userId, String marker) {
		super(id, token);
		setUserId(userId);
		setMarker(marker);
	}

	public int getUserId() {
		return getValue(USER_ID);
	}

	public void setUserId(int userId) {
		setValue(USER_ID, userId);
	}

	public String getMarker() {
		return getValue(MARKER);
	}

	public void setMarker(String marker) {
		setValue(MARKER, marker);
	}
}
