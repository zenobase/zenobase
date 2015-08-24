package com.zenobase.tasks.hexoskin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;

public class HexoskinSleepTask extends HexoskinTaskSupport {

	public static final String TYPE = "hexoskin-sleep";

	public HexoskinSleepTask(ObjectNode node) {
		super(node);
	}

	public HexoskinSleepTask(String bucketId, Identity principal, String tag, DateTimeZone zone, String marker) {
		super(TYPE, bucketId, principal, tag, zone, marker);
	}

	@Override
	public HexoskinSleepTask copy() {
		return copy(getClass());
	}
}
