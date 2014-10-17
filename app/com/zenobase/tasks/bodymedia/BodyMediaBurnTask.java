package com.zenobase.tasks.bodymedia;

import javax.measure.quantity.Energy;
import javax.measure.unit.Unit;

import org.elasticsearch.common.base.Objects;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Units;
import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaBurnTask extends Task {

	public static final String TYPE = "bodymedia-burn";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField HOURLY = new BooleanField("hourly");
	public static final TokenField ENERGY_UNIT = new TokenField("energy_unit");

	public BodyMediaBurnTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaBurnTask(String bucketId, Identity principal, String tag, boolean hourly, Unit<Energy> energyUnit, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(HOURLY, hourly);
		setSetting(ENERGY_UNIT, energyUnit.toString());
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public boolean isHourly() {
		return Objects.firstNonNull(getSetting(HOURLY), true);
	}

	public Unit<Energy> getEnergyUnit() {
		return Units.valueOf(Objects.firstNonNull(getSetting(ENERGY_UNIT), "cal"));
	}

	@Override
	public BodyMediaBurnTask copy() {
		return copy(getClass());
	}
}
