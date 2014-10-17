package com.zenobase.tasks.fitbit;

import javax.measure.quantity.Energy;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.common.Units;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class FitbitStepsTask extends Task {

	public static final String TYPE = "fitbit-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField ENERGY_UNIT = new TokenField("energy_unit");

	public FitbitStepsTask(ObjectNode node) {
		super(node);
	}

	FitbitStepsTask(String bucketId, Identity principal, String marker, String tag, Unit<Energy> energyUnit) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(ENERGY_UNIT, energyUnit.toString());
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public Unit<Energy> getEnergyUnit() {
		return Units.valueOf(Objects.firstNonNull(getSetting(ENERGY_UNIT), "cal"));
	}

	@Override
	public FitbitStepsTask copy() {
		return copy(getClass());
	}
}
