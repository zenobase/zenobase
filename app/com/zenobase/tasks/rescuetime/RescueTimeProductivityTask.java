package com.zenobase.tasks.rescuetime;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class RescueTimeProductivityTask extends Task {

	public static final String TYPE = "rescuetime-productivity";
	public static final TokenField KEY = new TokenField("key");
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public RescueTimeProductivityTask(ObjectNode node) {
		super(node);
	}

	public RescueTimeProductivityTask(String bucketId, Identity principal, String key, String tag, DateTimeZone timezone) {
		this(bucketId, principal, key, tag, timezone, null);
	}

	RescueTimeProductivityTask(String bucketId, Identity principal, String key, String tag, DateTimeZone timezone, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(KEY, checkNotNull(key));
		setSetting(TAG, checkNotNull(tag));
		setSetting(TIMEZONE, timezone.getID());
		setMarker(marker);
	}

	public String getKey() {
		return getSetting(KEY);
	}

	public String getTag() {
		return getSetting(TAG);
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
