package com.zenobase.tasks.forecast;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class ForecastTask extends Task {

	public static final String TYPE = "forecast";
	public static final BooleanField SI = new BooleanField("si");
	public static final BooleanField TAGS = new BooleanField("tags");

	public ForecastTask(ObjectNode node) {
		super(node);
	}

	public ForecastTask(String bucketId, Identity principal, boolean standardUnits, boolean addTags) {
		this(bucketId, principal, standardUnits, addTags, null);
	}

	ForecastTask(String bucketId, Identity principal, boolean standardUnits, boolean addTags, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(SI, standardUnits);
		setSetting(TAGS, addTags);
		setMarker(marker);
	}

	public boolean useStandardUnits() {
		return Objects.firstNonNull(getSetting(SI), Boolean.TRUE);
	}

	public boolean addTags() {
		return Objects.firstNonNull(getSetting(TAGS), Boolean.TRUE);
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null
			? DateTime.parse(marker, ISODateTimeFormat.dateTime().withOffsetParsed())
			: new DateTime(0L, DateTimeZone.UTC);
	}

	@Override
	public ForecastTask copy() {
		return copy(getClass());
	}
}
