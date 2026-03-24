package com.zenobase.tasks.mapmyfitness;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MapMyFitnessTask extends Task {

	public static final String TYPE = "mapmyfitness-activities";

	public MapMyFitnessTask(ObjectNode node) {
		super(node);
	}

	public MapMyFitnessTask(String bucketId, Identity principal) {
		this(bucketId, principal, null);
	}

	MapMyFitnessTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public MapMyFitnessTask copy() {
		return copy(getClass());
	}
}
