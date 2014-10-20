package com.zenobase.tasks.runkeeper;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.joda.time.DateTimeZone;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.common.Units;
import com.zenobase.json.TokenField;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class RunkeeperTask extends Task {

	public static final String TYPE = "runkeeper-activities";
	public static final UnitField<Length> LENGTH_UNIT = new UnitField<Length>("unit");
	public static final UnitField<Energy> ENERGY_UNIT = new UnitField<Energy>("energy_unit");
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
		setSetting(LENGTH_UNIT, lengthUnit);
		setSetting(ENERGY_UNIT, energyUnit);
	}

	public DateTimeZone getTimezone() {
		String value = getSetting(TIMEZONE);
		return value != null ? DateTimeZone.forID(value) : DateTimeZone.UTC;
	}

	public Unit<Length> getDistanceUnit() {
		return getSetting(LENGTH_UNIT);
	}

	public Unit<Length> getHeightUnit() {
		return Units.isMetric(getDistanceUnit()) ? Units.M : Units.FT;
	}

	public Unit<Energy> getEnergyUnit() {
		return Objects.firstNonNull(getSetting(ENERGY_UNIT), Units.CAL);
	}

	@Override
	public RunkeeperTask copy() {
		return copy(getClass());
	}
}
