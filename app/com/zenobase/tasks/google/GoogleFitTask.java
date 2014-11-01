package com.zenobase.tasks.google;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class GoogleFitTask extends Task {

	public static final String TYPE = "google-activities";
	public static final TokenField TIMEZONE = new TokenField("timezone");
	public static final BooleanField METRIC = new BooleanField("metric");
	public static final BooleanField DERIVED = new BooleanField("derived");

	public GoogleFitTask(ObjectNode node) {
		super(node);
	}

	public GoogleFitTask(String bucketId, Identity principal, DateTimeZone timezone, boolean metric, boolean derived, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(TIMEZONE, timezone != null ? timezone.getID() : null);
		setSetting(METRIC, metric);
		setSetting(DERIVED, derived);
		setMarker(marker);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public boolean isMetric() {
		return getSetting(METRIC);
	}

	public boolean useDerived() {
		return getSetting(DERIVED);
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker, ISODateTimeFormat.dateTime().withOffsetParsed()) : null;
	}

	@Override
	public GoogleFitTask copy() {
		return copy(getClass());
	}
}
