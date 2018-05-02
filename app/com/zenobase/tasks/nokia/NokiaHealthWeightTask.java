package com.zenobase.tasks.nokia;

import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTimeZone;

import com.zenobase.json.TokenField;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class NokiaHealthWeightTask extends Task {

	public static final String TYPE = "nokia-weight";
	public static final TokenField TAG = new TokenField("tag");
	public static final UnitField<Mass> UNIT = new UnitField<>("unit");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public NokiaHealthWeightTask(ObjectNode node) {
		super(node);
	}

	NokiaHealthWeightTask(String bucketId, Identity principal, String tag, Unit<Mass> unit, DateTimeZone timezone, String marker) {
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
	public NokiaHealthWeightTask copy() {
		return copy(getClass());
	}
}
