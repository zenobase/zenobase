package com.zenobase.tasks.google;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTimeZone;

import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;

public class GoogleFitActivitiesTask extends GoogleFitTaskSupport {

	public static final String TYPE = "google-activities";
	public static final BooleanField METRIC = new BooleanField("metric");
	public static final BooleanField DERIVED = new BooleanField("derived");

	public GoogleFitActivitiesTask(ObjectNode node) {
		super(node);
	}

	public GoogleFitActivitiesTask(
		String bucketId,
		Identity principal,
		DateTimeZone timezone,
		boolean metric,
		boolean derived,
		String marker
	) {
		super(TYPE, bucketId, principal, timezone, marker);
		setSetting(METRIC, metric);
		setSetting(DERIVED, derived);
	}

	public boolean isMetric() {
		return MoreObjects.firstNonNull(getSetting(METRIC), false);
	}

	public boolean useDerived() {
		return MoreObjects.firstNonNull(getSetting(DERIVED), false);
	}

	@Override
	public GoogleFitActivitiesTask copy() {
		return copy(getClass());
	}
}
