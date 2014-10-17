package com.zenobase.tasks.withings;

import javax.measure.quantity.Energy;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.common.Measures;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class WithingsStepsTask extends Task {

	public static final String TYPE = "withings-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField LENGTH_UNIT = new TokenField("unit");
	public static final TokenField ENERGY_UNIT = new TokenField("energy_unit");

	public WithingsStepsTask(ObjectNode node) {
		super(node);
	}

	WithingsStepsTask(String bucketId, Identity principal, String tag, Unit<Length> lengthUnit, Unit<Energy> energyUnit, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(LENGTH_UNIT, lengthUnit.toString());
		setSetting(ENERGY_UNIT, energyUnit.toString());
	}

	public String getTag() {
		return getSetting(TAG);
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
	public WithingsStepsTask copy() {
		return copy(getClass());
	}
}
