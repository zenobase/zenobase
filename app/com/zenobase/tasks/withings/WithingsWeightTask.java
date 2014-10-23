package com.zenobase.tasks.withings;

import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class WithingsWeightTask extends Task {

	public static final String TYPE = "withings-weight";
	public static final TokenField TAG = new TokenField("tag");
	public static final UnitField<Mass> UNIT = new UnitField<Mass>("unit");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public WithingsWeightTask(ObjectNode node) {
		super(node);
	}

	WithingsWeightTask(String bucketId, Identity principal, String tag, Unit<Mass> unit, DateTimeZone timezone, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(UNIT, unit);
		setSetting(TIMEZONE, timezone.getID());
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public Unit<Mass> getUnit() {
		return getSetting(UNIT);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	@Override
	public WithingsWeightTask copy() {
		return copy(getClass());
	}
}
