package com.zenobase.tasks.ihealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Identity;

public class IHealthSleepTask extends IHealthTaskSupport {

	public static final String TYPE = "ihealth-sleep";

	public IHealthSleepTask(ObjectNode node) {
		super(node);
	}

	public IHealthSleepTask(
			String bucketId, Identity principal, @Nullable String tag, DateTimeZone zone, String marker) {
		super(TYPE, bucketId, principal, tag, zone, marker);
	}
}
