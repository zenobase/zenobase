package com.zenobase.tasks.runkeeper;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.common.Measures;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class RunkeeperTask extends Task {

	public static final String TYPE = "runkeeper-activities";
	public static final TokenField LENGTH_UNIT = new TokenField("unit");
	public static final TokenField ENERGY_UNIT = new TokenField("energy_unit");
	public static final TokenField TIMEZONE = new TokenField("timezone");

	public RunkeeperTask(ObjectNode node) {
		super(node);
	}

	public RunkeeperTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	RunkeeperTask(String bucketId, Identity principal, DateTimeZone timezone, Unit<Length> lengthUnit, Unit<Energy> energyUnit, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TIMEZONE, timezone != null ? timezone.getID() : null);
		setSetting(LENGTH_UNIT, lengthUnit.toString());
		setSetting(ENERGY_UNIT, energyUnit.toString());
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public Unit<Length> getDistanceUnit() {
		return Measures.<Length>parseUnit(getSetting(LENGTH_UNIT));
	}

	public Unit<Length> getHeightUnit() {
		return Measures.isMetric(getDistanceUnit()) ? SI.METER : NonSI.FOOT;
	}

	public Unit<Energy> getEnergyUnit() {
		return Measures.parseUnit(Objects.firstNonNull(getSetting(ENERGY_UNIT), "cal"));
	}

	@Override
	public RunkeeperTask copy() {
		return copy(getClass());
	}
}
