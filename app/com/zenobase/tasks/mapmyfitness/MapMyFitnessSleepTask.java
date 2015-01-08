package com.zenobase.tasks.mapmyfitness;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MapMyFitnessSleepTask extends Task {

	public static final String TYPE = "mapmyfitness-sleep";

	public MapMyFitnessSleepTask(ObjectNode node) {
		super(node);
	}

	public MapMyFitnessSleepTask(String bucketId, Identity principal) {
		this(bucketId, principal, null);
	}

	MapMyFitnessSleepTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public MapMyFitnessSleepTask copy() {
		return copy(getClass());
	}
}
