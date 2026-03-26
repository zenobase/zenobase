package com.zenobase.tasks.rescuetime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class RescueTimeProductivityTask extends Task {

	public static final String TYPE = "rescuetime-productivity";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField KIND = new TokenField("kind");
	public static final TokenField SOURCE = new TokenField("source");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public RescueTimeProductivityTask(ObjectNode node) {
		super(node);
	}

	public RescueTimeProductivityTask(
			String bucketId,
			Identity principal,
			@Nullable String tag,
			@Nullable String kind,
			@Nullable String source,
			DateTimeZone timezone) {
		this(bucketId, principal, tag, kind, source, timezone, null);
	}

	RescueTimeProductivityTask(
			String bucketId,
			Identity principal,
			@Nullable String tag,
			@Nullable String kind,
			@Nullable String source,
			DateTimeZone timezone,
			@Nullable String marker) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setSetting(KIND, kind);
		setSetting(SOURCE, source);
		setSetting(TIMEZONE, timezone.getID());
		setMarker(marker);
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	public String getKind() {
		return MoreObjects.firstNonNull(getSetting(KIND), "efficiency");
	}

	public @Nullable String getSource() {
		return getSetting(SOURCE);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public @Nullable DateTime getLast() {
		String marker = getMarker();
		return marker != null
				? DateTime.parse(marker, ISODateTimeFormat.dateTime().withOffsetParsed())
				: null;
	}

	@Override
	public RescueTimeProductivityTask copy() {
		return copy(getClass());
	}
}
