package com.zenobase.tasks.microsoft;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

abstract class MicrosoftHealthTaskSupport extends Task {

	public static final TokenField TIMEZONE = new TokenField("timezone");

	protected MicrosoftHealthTaskSupport(ObjectNode node) {
		super(node);
	}

	MicrosoftHealthTaskSupport(String type, String bucketId, Identity principal, DateTimeZone zone, DateTime marker) {
		super(type, bucketId, principal);
		setSetting(TIMEZONE, zone != null ? zone.getID() : null);
		setMarker(marker.toString());
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public DateTime getFrom() {
		return DateTime.parse(getMarker());
	}

	@Override
	public MicrosoftHealthTaskSupport copy() {
		return copy(getClass());
	}
}
