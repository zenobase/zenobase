package com.zenobase.tasks.moves;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.common.Units;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MovesActivitiesTask extends Task {

	public static final String TYPE = "moves-activities";
	public static final UnitField<Length> LENGTH_UNIT = new UnitField<>("unit");
	public static final UnitField<Energy> ENERGY_UNIT = new UnitField<>("energy_unit");

	public MovesActivitiesTask(ObjectNode node) {
		super(node);
	}

	public MovesActivitiesTask(String bucketId, Identity principal, Unit<Length> lengthUnit, Unit<Energy> energyUnit) {
		super(TYPE, bucketId, principal);
		setSetting(LENGTH_UNIT, lengthUnit);
		setSetting(ENERGY_UNIT, energyUnit);
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public Unit<Length> getUnit() {
		return getSetting(LENGTH_UNIT);
	}

	public Unit<Energy> getEnergyUnit() {
		return Objects.firstNonNull(getSetting(ENERGY_UNIT), Units.CAL);
	}

	@Override
	public MovesActivitiesTask copy() {
		return copy(getClass());
	}
}
