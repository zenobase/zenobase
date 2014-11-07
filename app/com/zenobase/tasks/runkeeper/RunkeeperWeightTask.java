package com.zenobase.tasks.runkeeper;

import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class RunkeeperWeightTask extends Task {

	public static final String TYPE = "runkeeper-weight";

	private static final TokenField TAG = new TokenField("tag");
	private static final UnitField<Mass> UNIT = new UnitField<Mass>("unit");
	private static final TokenField TIMEZONE = new TokenField("timezone");

	public RunkeeperWeightTask(ObjectNode node) {
		super(node);
	}

	public RunkeeperWeightTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	RunkeeperWeightTask(String bucketId, Identity principal, String tag, Unit<Mass> unit, DateTimeZone timezone, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(UNIT, unit);
		setSetting(TIMEZONE, timezone != null ? timezone.getID() : null);
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
	public RunkeeperWeightTask copy() {
		return copy(getClass());
	}
}
