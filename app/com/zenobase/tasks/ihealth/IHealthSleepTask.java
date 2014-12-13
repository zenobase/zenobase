package com.zenobase.tasks.ihealth;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;

public class IHealthSleepTask extends IHealthTaskSupport {

	public static final String TYPE = "ihealth-sleep";

	public IHealthSleepTask(ObjectNode node) {
		super(node);
	}

	public IHealthSleepTask(String bucketId, Identity principal, String tag, DateTimeZone zone, String marker) {
		super(TYPE, bucketId, principal, tag, zone, marker);
	}
}
