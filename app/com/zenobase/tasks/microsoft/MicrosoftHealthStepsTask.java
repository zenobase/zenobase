package com.zenobase.tasks.microsoft;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import org.joda.time.DateTimeZone;

public class MicrosoftHealthStepsTask extends MicrosoftHealthTaskSupport {

	public static final String TYPE = "microsoft-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField HOURLY = new BooleanField("hourly");
	public static final BooleanField METRIC = new BooleanField("metric");

	public MicrosoftHealthStepsTask(ObjectNode node) {
		super(node);
	}

	MicrosoftHealthStepsTask(String bucketId, Identity principal, DateTimeZone zone, String tag, boolean hourly, boolean metric, String marker) {
		super(TYPE, bucketId, principal, zone, marker);
		setSetting(TAG, tag);
		setSetting(HOURLY, hourly);
		setSetting(METRIC, metric);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public boolean isHourly() {
		return Objects.firstNonNull(getSetting(HOURLY), false);
	}

	public boolean isMetric() {
		return Objects.firstNonNull(getSetting(METRIC), Boolean.TRUE);
	}

	@Override
	public MicrosoftHealthStepsTask copy() {
		return copy(getClass());
	}
}
