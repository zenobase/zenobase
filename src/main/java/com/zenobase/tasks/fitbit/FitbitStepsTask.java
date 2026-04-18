package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.common.Units;
import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import java.util.Objects;
import javax.measure.quantity.Energy;
import javax.measure.unit.Unit;

public class FitbitStepsTask extends Task {

	public static final String TYPE = "fitbit-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField HOURLY = new BooleanField("hourly");
	public static final BooleanField INCLUDE_BMR = new BooleanField("bmr");
	public static final UnitField<Energy> ENERGY_UNIT = new UnitField<>("energy_unit");

	public FitbitStepsTask(ObjectNode node) {
		super(node);
	}

	FitbitStepsTask(
		String bucketId,
		Identity principal,
		String marker,
		String tag,
		boolean hourly,
		Unit<Energy> energyUnit
	) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(HOURLY, hourly);
		setSetting(ENERGY_UNIT, energyUnit);
		setSetting(INCLUDE_BMR, true);
	}

	public String getTag() {
		return Objects.requireNonNull(getSetting(TAG));
	}

	public boolean isHourly() {
		return MoreObjects.firstNonNull(getSetting(HOURLY), false);
	}

	public boolean includeBMR() {
		return MoreObjects.firstNonNull(getSetting(INCLUDE_BMR), false);
	}

	public Unit<Energy> getEnergyUnit() {
		return MoreObjects.firstNonNull(getSetting(ENERGY_UNIT), Units.CAL);
	}

	@Override
	public FitbitStepsTask copy() {
		return copy(getClass());
	}
}
