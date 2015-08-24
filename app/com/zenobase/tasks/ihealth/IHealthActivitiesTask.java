package com.zenobase.tasks.ihealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;

public class IHealthActivitiesTask extends IHealthTaskSupport {

	public static final String TYPE = "ihealth-activities";

	public IHealthActivitiesTask(ObjectNode node) {
		super(node);
	}

	public IHealthActivitiesTask(String bucketId, Identity principal, String tag, DateTimeZone zone, String marker) {
		super(TYPE, bucketId, principal, tag, zone, marker);
	}
}
