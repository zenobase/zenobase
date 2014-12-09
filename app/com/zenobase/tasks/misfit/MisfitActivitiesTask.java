package com.zenobase.tasks.misfit;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MisfitActivitiesTask extends Task {

	public static final String TYPE = "misfit-activities";

	public MisfitActivitiesTask(ObjectNode node) {
		super(node);
	}

	public MisfitActivitiesTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	public DateTime getBegin() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	@Override
	public MisfitActivitiesTask copy() {
		return copy(getClass());
	}
}
