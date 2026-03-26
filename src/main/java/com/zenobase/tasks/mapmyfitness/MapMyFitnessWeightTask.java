package com.zenobase.tasks.mapmyfitness;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MapMyFitnessWeightTask extends Task {

	public static final String TYPE = "mapmyfitness-weight";
	public static final TokenField TAG = new TokenField("tag");

	public MapMyFitnessWeightTask(ObjectNode node) {
		super(node);
	}

	public MapMyFitnessWeightTask(String bucketId, Identity principal, String tag) {
		this(bucketId, principal, tag, null);
	}

	MapMyFitnessWeightTask(String bucketId, Identity principal, String tag, @Nullable String marker) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setMarker(marker);
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	@Override
	public MapMyFitnessWeightTask copy() {
		return copy(getClass());
	}
}
