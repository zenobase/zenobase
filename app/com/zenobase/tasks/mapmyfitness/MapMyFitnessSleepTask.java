package com.zenobase.tasks.mapmyfitness;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MapMyFitnessSleepTask extends Task {

	public static final String TYPE = "mapmyfitness-sleep";
	public static final TokenField TAG = new TokenField("tag");

	public MapMyFitnessSleepTask(ObjectNode node) {
		super(node);
	}

	public MapMyFitnessSleepTask(String bucketId, Identity principal, String tag) {
		this(bucketId, principal, tag, null);
	}

	MapMyFitnessSleepTask(String bucketId, Identity principal, String tag, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setMarker(marker);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	@Override
	public MapMyFitnessSleepTask copy() {
		return copy(getClass());
	}
}
