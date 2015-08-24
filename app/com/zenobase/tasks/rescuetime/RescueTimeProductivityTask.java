package com.zenobase.tasks.rescuetime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

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

	public RescueTimeProductivityTask(String bucketId, Identity principal, String tag, String kind, String source, DateTimeZone timezone) {
		this(bucketId, principal, tag, kind, source, timezone, null);
	}

	RescueTimeProductivityTask(String bucketId, Identity principal, String tag, String kind, String source, DateTimeZone timezone, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setSetting(KIND, kind);
		setSetting(SOURCE, source);
		setSetting(TIMEZONE, timezone.getID());
		setMarker(marker);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public String getKind() {
		return Objects.firstNonNull(getSetting(KIND), "efficiency");
	}

	public String getSource() {
		return getSetting(SOURCE);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public DateTime getLast() {
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
