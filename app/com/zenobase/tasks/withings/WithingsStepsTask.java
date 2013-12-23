package com.zenobase.tasks.withings;

import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class WithingsStepsTask extends Task {

	public static final String TYPE = "withings";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField UNIT = new TokenField("unit");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public WithingsStepsTask(ObjectNode node) {
		super(node);
	}

	WithingsStepsTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public void setTag(String tag) {
		setSetting(TAG, tag);
	}

	public Unit<Length> getUnit() {
		return Measures.<Length>parseUnit(getSetting(UNIT));
	}

	public void setUnit(Unit<Mass> unit) {
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
	public WithingsStepsTask copy() {
		return copy(getClass());
	}
}
