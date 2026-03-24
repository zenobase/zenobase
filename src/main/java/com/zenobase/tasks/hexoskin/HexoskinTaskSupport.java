package com.zenobase.tasks.hexoskin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

abstract class HexoskinTaskSupport extends Task {

	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public HexoskinTaskSupport(ObjectNode node) {
		super(node);
	}

	public HexoskinTaskSupport(String type, String bucketId, Identity principal, String tag, DateTimeZone zone, String marker) {
		super(type, bucketId, principal);
		setSetting(TAG, tag);
		setSetting(TIMEZONE, zone.toString());
		setMarker(marker);
	}

	public long getStart() {
		return DateTime.parse(getMarker()).getMillis() * 256 / 1000;
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public DateTimeZone getZone() {
		return DateTimeZone.forID(getSetting(TIMEZONE));
	}

	@Override
	public HexoskinTaskSupport copy() {
		return copy(getClass());
	}
}
