package com.zenobase.tasks.nokia;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.common.Units;
import com.zenobase.json.TokenField;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class NokiaHealthStepsTask extends Task {

	public static final String TYPE = "nokia-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final UnitField<Length> LENGTH_UNIT = new UnitField<>("unit");
	public static final UnitField<Energy> ENERGY_UNIT = new UnitField<>("energy_unit");

	public NokiaHealthStepsTask(ObjectNode node) {
		super(node);
	}

	NokiaHealthStepsTask(String bucketId, Identity principal, String tag, Unit<Length> lengthUnit, Unit<Energy> energyUnit, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(LENGTH_UNIT, lengthUnit);
		setSetting(ENERGY_UNIT, energyUnit);
	}

	public String getTag() {
		return getSetting(TAG);
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
	public NokiaHealthStepsTask copy() {
		return copy(getClass());
	}
}
