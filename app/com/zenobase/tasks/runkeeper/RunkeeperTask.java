package com.zenobase.tasks.runkeeper;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class RunkeeperTask extends Task {

	public static final String TYPE = "runkeeper-activities";
	public static final TokenField UNIT = new TokenField("unit");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public RunkeeperTask(ObjectNode node) {
		super(node);
	}

	public RunkeeperTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	RunkeeperTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	public Unit<Length> getUnit() {
		return Measures.<Length>parseUnit(getSetting(UNIT));
	}

	public void setUnit(Unit<Length> unit) {
		setSetting(UNIT, unit.toString());
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public void setTimezone(DateTimeZone timezone) {
		setSetting(TIMEZONE, timezone != null ? timezone.getID() : null);
	}

	@Override
	public RunkeeperTask copy() {
		return copy(getClass());
	}
}
