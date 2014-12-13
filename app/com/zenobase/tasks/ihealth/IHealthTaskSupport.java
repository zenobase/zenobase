package com.zenobase.tasks.ihealth;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

abstract class IHealthTaskSupport extends Task {

	private static final TokenField TAG = new TokenField("tag");
	private static final TokenField TIMEZONE = new TokenField("timezone");

	public IHealthTaskSupport(ObjectNode node) {
		super(node);
	}

	protected IHealthTaskSupport(String type, String bucketId, Identity principal, String tag, DateTimeZone zone, String marker) {
		super(type, bucketId, principal);
		setSetting(TAG, tag);
		setSetting(TIMEZONE, zone.getID());
		setMarker(marker);
	}

	public DateTime getBegin() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}
}
