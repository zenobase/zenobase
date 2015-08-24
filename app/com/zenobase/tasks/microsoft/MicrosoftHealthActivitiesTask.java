package com.zenobase.tasks.microsoft;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;

public class MicrosoftHealthActivitiesTask extends MicrosoftHealthTaskSupport {

	public static final String TYPE = "microsoft-activities";
	public static final BooleanField METRIC = new BooleanField("metric");

	public MicrosoftHealthActivitiesTask(ObjectNode node) {
		super(node);
	}

	MicrosoftHealthActivitiesTask(String bucketId, Identity principal, DateTimeZone zone, boolean metric, DateTime marker) {
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
