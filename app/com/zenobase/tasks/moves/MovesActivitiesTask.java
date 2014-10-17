package com.zenobase.tasks.moves;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.common.Units;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MovesActivitiesTask extends Task {

	public static final String TYPE = "moves-activities";
	public static final TokenField LENGTH_UNIT = new TokenField("unit");
	public static final TokenField ENERGY_UNIT = new TokenField("energy_unit");

	public MovesActivitiesTask(ObjectNode node) {
		super(node);
	}

	public MovesActivitiesTask(String bucketId, Identity principal, Unit<Length> lengthUnit, Unit<Energy> energyUnit) {
		super(TYPE, bucketId, principal);
		setSetting(LENGTH_UNIT, lengthUnit.toString());
		setSetting(ENERGY_UNIT, energyUnit.toString());
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public Unit<Length> getUnit() {
		return Units.<Length>valueOf(getSetting(LENGTH_UNIT));
	}

	public Unit<Energy> getEnergyUnit() {
		return Units.valueOf(Objects.firstNonNull(getSetting(ENERGY_UNIT), "cal"));
	}

	@Override
	public MovesActivitiesTask copy() {
		return copy(getClass());
	}
}
