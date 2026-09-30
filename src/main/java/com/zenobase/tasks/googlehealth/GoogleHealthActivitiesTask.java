package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;
import java.util.Objects;
import org.joda.time.DateTimeZone;

public class GoogleHealthActivitiesTask extends GoogleHealthTaskSupport {

	public static final String TYPE = "google-health-activities";
	public static final BooleanField METRIC = new BooleanField("metric");

	public GoogleHealthActivitiesTask(ObjectNode node) {
		super(node);
	}

	public GoogleHealthActivitiesTask(
		String bucketId,
		Identity principal,
		DateTimeZone timezone,
		boolean metric,
		String marker
	) {
		super(TYPE, bucketId, principal, timezone, marker);
		setSetting(METRIC, metric);
	}

	public boolean isMetric() {
		return Objects.requireNonNull(getSetting(METRIC));
	}

	@Override
	public GoogleHealthActivitiesTask copy() {
		return copy(getClass());
	}
}
