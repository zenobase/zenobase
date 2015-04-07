package com.zenobase.tasks.moves;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MovesPlacesTask extends Task {

	public static final String TYPE = "moves-places";
	public static final TokenField TAG = new TokenField("tag");

	public MovesPlacesTask(ObjectNode node) {
		super(node);
	}

	public MovesPlacesTask(String bucketId, Identity principal, String tag) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public String getTag() {
		return Objects.firstNonNull(getSetting(TAG), "Place");
	}

	@Override
	public MovesPlacesTask copy() {
		return copy(getClass());
	}
}
