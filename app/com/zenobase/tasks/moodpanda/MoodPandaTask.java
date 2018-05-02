package com.zenobase.tasks.moodpanda;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MoodPandaTask extends Task {

	public static final String TYPE = "moodpanda";
	public static final TokenField EMAIL = new TokenField("email");
	public static final TokenField TAG = new TokenField("tag");

	public MoodPandaTask(ObjectNode node) {
		super(node);
	}

	MoodPandaTask(String bucketId, Identity principal, String email, String tag, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(EMAIL, email);
		setSetting(TAG, tag);
		setMarker(marker);
	}

	public String getEmail() {
		return getSetting(EMAIL);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	@Override
	public MoodPandaTask copy() {
		return copy(getClass());
	}
}
