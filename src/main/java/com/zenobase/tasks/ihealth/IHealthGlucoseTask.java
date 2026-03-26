package com.zenobase.tasks.ihealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Identity;

public class IHealthGlucoseTask extends IHealthTaskSupport {

	public static final String TYPE = "ihealth-glucose";

	public IHealthGlucoseTask(ObjectNode node) {
		super(node);
	}

	public IHealthGlucoseTask(
			String bucketId, Identity principal, @Nullable String tag, DateTimeZone zone, String marker) {
		super(TYPE, bucketId, principal, tag, zone, marker);
	}
}
