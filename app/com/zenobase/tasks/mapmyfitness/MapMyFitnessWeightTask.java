package com.zenobase.tasks.mapmyfitness;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MapMyFitnessWeightTask extends Task {

	public static final String TYPE = "mapmyfitness-weight";

	public MapMyFitnessWeightTask(ObjectNode node) {
		super(node);
	}

	public MapMyFitnessWeightTask(String bucketId, Identity principal) {
		this(bucketId, principal, null);
	}

	MapMyFitnessWeightTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public MapMyFitnessWeightTask copy() {
		return copy(getClass());
	}
}
