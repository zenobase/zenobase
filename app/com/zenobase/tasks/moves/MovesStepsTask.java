package com.zenobase.tasks.moves;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MovesStepsTask extends Task {

	public static final String TYPE = "moves-steps";
	public static final TokenField TAG = new TokenField("tag");
	public static final UnitField<Length> LENGTH_UNIT = new UnitField<>("unit");

	public MovesStepsTask(ObjectNode node) {
		super(node);
	}

	public MovesStepsTask(String bucketId, Identity principal, String tag, Unit<Length> lengthUnit) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setSetting(LENGTH_UNIT, lengthUnit);
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public Unit<Length> getUnit() {
		return getSetting(LENGTH_UNIT);
	}

	@Override
	public MovesStepsTask copy() {
		return copy(getClass());
	}
}
