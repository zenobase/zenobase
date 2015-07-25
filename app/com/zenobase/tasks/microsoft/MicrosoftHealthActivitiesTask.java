package com.zenobase.tasks.microsoft;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import org.joda.time.DateTimeZone;

public class MicrosoftHealthActivitiesTask extends Task {

	public static final String TYPE = "microsoft-activities";
	public static final TokenField TIMEZONE = new TokenField("timezone");
	public static final BooleanField METRIC = new BooleanField("metric");

	public MicrosoftHealthActivitiesTask(ObjectNode node) {
		super(node);
	}

	public MicrosoftHealthActivitiesTask(String bucketId, Identity principal, DateTimeZone zone, boolean metric) {
		this(bucketId, principal, zone, metric, null);
	}

	MicrosoftHealthActivitiesTask(String bucketId, Identity principal, DateTimeZone zone, boolean metric, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(METRIC, metric);
		setSetting(TIMEZONE, zone != null ? zone.getID() : null);
		setMarker(marker);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public boolean isMetric() {
		return Objects.firstNonNull(getSetting(METRIC), Boolean.TRUE);
	}

	@Override
	public MicrosoftHealthActivitiesTask copy() {
		return copy(getClass());
	}
}
