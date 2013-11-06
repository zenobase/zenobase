package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class FitbitIntradayTask extends Task {

	public static final String TYPE = "fitbit-intraday";

	public FitbitIntradayTask(ObjectNode node) {
		super(node);
	}

	FitbitIntradayTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public FitbitIntradayTask copy() {
		return copy(getClass());
	}
}
