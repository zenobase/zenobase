package com.zenobase.tasks.withings;

import javax.measure.quantity.Length;
import javax.measure.quantity.Mass;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class WithingsStepsTask extends Task {

	public static final String TYPE = "withings-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final TokenField UNIT = new TokenField("unit");

	public WithingsStepsTask(ObjectNode node) {
		super(node);
	}

	WithingsStepsTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public void setTag(String tag) {
		setSetting(TAG, tag);
	}

	public Unit<Length> getDistanceUnit() {
		return Measures.<Length>parseUnit(getSetting(UNIT));
	}

	public Unit<Length> getHeightUnit() {
		return Measures.isMetric(getDistanceUnit()) ? SI.METER : NonSI.FOOT;
	}

	public void setUnit(Unit<Mass> unit) {
		setSetting(UNIT, unit.toString());
	}

	@Override
	public WithingsStepsTask copy() {
		return copy(getClass());
	}
}
