package com.zenobase.tasks.hexoskin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Identity;

public class HexoskinActivitiesTask extends HexoskinTaskSupport {

	public static final String TYPE = "hexoskin-activities";

	public HexoskinActivitiesTask(ObjectNode node) {
		super(node);
	}

	public HexoskinActivitiesTask(
		String bucketId,
		Identity principal,
		@Nullable String tag,
		DateTimeZone zone,
		String marker
	) {
		super(TYPE, bucketId, principal, tag, zone, marker);
	}

	@Override
	public HexoskinActivitiesTask copy() {
		return copy(getClass());
	}
}
