package com.zenobase.tasks.forecast;

import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class ForecastTask extends Task {

	public static final String TYPE = "forecast";
	public static final BooleanField SI = new BooleanField("si");
	public static final BooleanField TAGS = new BooleanField("tags"); // TODO deprecate
	public static final TokenField FIELDS = new TokenField("fields");

	public ForecastTask(ObjectNode node) {
		super(node);
	}

	public ForecastTask(String bucketId, Identity principal, Set<String> fields, boolean standardUnits) {
		this(bucketId, principal, fields, standardUnits, null);
	}

	ForecastTask(String bucketId, Identity principal, Set<String> fields, boolean standardUnits, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(SI, standardUnits);
		setSettings(FIELDS, fields);
		setMarker(marker);
	}

	public boolean useStandardUnits() {
		return Objects.firstNonNull(getSetting(SI), Boolean.TRUE);
	}

	public Set<String> getFields() {
		Set<String> fields = Sets.newHashSet(Objects.firstNonNull(getSettings(FIELDS), ImmutableSet.of()));
		if (fields.isEmpty()) {
			fields.add(Event.TEMPERATURE.getName());
			fields.add(Event.PRESSURE.getName());
			fields.add(Event.HUMIDITY.getName());
		}
		if (getSetting(TAGS) == Boolean.TRUE) {
			fields.add("tag");
		}
		return fields;
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
