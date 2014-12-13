package com.zenobase.tasks.ihealth;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;

public class IHealthGlucoseTask extends IHealthTaskSupport {

	public static final String TYPE = "ihealth-glucose";

	public IHealthGlucoseTask(ObjectNode node) {
		super(node);
	}

	public IHealthGlucoseTask(String bucketId, Identity principal, String tag, DateTimeZone zone, String marker) {
		super(TYPE, bucketId, principal, tag, zone, marker);
	}
}
