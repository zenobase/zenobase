package com.zenobase.tasks.ihealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Identity;

public class IHealthCardioTask extends IHealthTaskSupport {

	public static final String TYPE = "ihealth-cardio";

	public IHealthCardioTask(ObjectNode node) {
		super(node);
	}

	public IHealthCardioTask(
			String bucketId, Identity principal, @Nullable String tag, DateTimeZone zone, String marker) {
		super(TYPE, bucketId, principal, tag, zone, marker);
	}
}
