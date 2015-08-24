package com.zenobase.tasks.ihealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Identity;

public class IHealthFoodTask extends IHealthTaskSupport {

	public static final String TYPE = "ihealth-food";

	public IHealthFoodTask(ObjectNode node) {
		super(node);
	}

	public IHealthFoodTask(String bucketId, Identity principal, String tag, DateTimeZone zone, String marker) {
		super(TYPE, bucketId, principal, tag, zone, marker);
	}
}
