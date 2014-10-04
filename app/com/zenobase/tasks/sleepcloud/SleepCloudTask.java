package com.zenobase.tasks.sleepcloud;

import org.elasticsearch.common.base.Objects;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class SleepCloudTask extends Task {

	public static final String TYPE = "sleepcloud";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField RANGES = new BooleanField("ranges");

	public SleepCloudTask(ObjectNode node) {
		super(node);
	}

	public SleepCloudTask(String bucketId, Identity principal, String tag) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setSetting(RANGES, true);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker, ISODateTimeFormat.dateTime().withOffsetParsed()) : null;
	}

	public boolean useRanges() {
		return Objects.firstNonNull(getSetting(RANGES), false);
	}

	@Override
	public SleepCloudTask copy() {
		return copy(getClass());
	}
}
