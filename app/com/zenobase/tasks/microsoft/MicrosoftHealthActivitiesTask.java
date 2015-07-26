package com.zenobase.tasks.microsoft;

import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import org.joda.time.DateTimeZone;

public class MicrosoftHealthActivitiesTask extends MicrosoftHealthTaskSupport {

	public static final String TYPE = "microsoft-activities";
	public static final BooleanField METRIC = new BooleanField("metric");

	public MicrosoftHealthActivitiesTask(ObjectNode node) {
		super(node);
	}

	public MicrosoftHealthActivitiesTask(String bucketId, Identity principal, DateTimeZone zone, boolean metric) {
		this(bucketId, principal, zone, metric, null);
	}

	MicrosoftHealthActivitiesTask(String bucketId, Identity principal, DateTimeZone zone, boolean metric, String marker) {
		super(TYPE, bucketId, principal, zone, marker);
		setSetting(METRIC, metric);
	}

	public boolean isMetric() {
		return Objects.firstNonNull(getSetting(METRIC), Boolean.TRUE);
	}

	@Override
	public MicrosoftHealthActivitiesTask copy() {
		return copy(getClass());
	}
}
