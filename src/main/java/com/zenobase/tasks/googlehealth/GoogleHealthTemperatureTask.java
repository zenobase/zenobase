package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import java.util.Objects;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class GoogleHealthTemperatureTask extends GoogleHealthTaskSupport {

	public static final String TYPE = "google-health-temperature";
	public static final BooleanField METRIC = new BooleanField("metric");
	public static final TokenField TAG = new TokenField("tag");

	public GoogleHealthTemperatureTask(ObjectNode node) {
		super(node);
	}

	public GoogleHealthTemperatureTask(
		String bucketId,
		Identity principal,
		DateTimeZone timezone,
		boolean metric,
		String tag,
		String marker
	) {
		super(TYPE, bucketId, principal, timezone, marker);
		setSetting(METRIC, metric);
		setSetting(TAG, tag);
	}

	public boolean isMetric() {
		return Objects.requireNonNull(getSetting(METRIC));
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	@Override
	public GoogleHealthTemperatureTask copy() {
		return copy(getClass());
	}
}
