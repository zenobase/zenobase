package com.zenobase.tasks.moves;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MovesPlacesTask extends Task {

	public static final String TYPE = "moves-places";

	public MovesPlacesTask(ObjectNode node) {
		super(node);
	}

	public MovesPlacesTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	@Override
	public MovesPlacesTask copy() {
		return copy(getClass());
	}
}
