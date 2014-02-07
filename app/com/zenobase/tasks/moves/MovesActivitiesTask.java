package com.zenobase.tasks.moves;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MovesActivitiesTask extends Task {

	public static final String TYPE = "moves-activities";
	public static final TokenField UNIT = new TokenField("unit");

	public MovesActivitiesTask(ObjectNode node) {
		super(node);
	}

	public MovesActivitiesTask(String bucketId, Identity principal, Unit<Length> unit) {
		super(TYPE, bucketId, principal);
		setSetting(UNIT, unit.toString());
	}

	public DateTime getFrom() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public Unit<Length> getUnit() {
		return Measures.<Length>parseUnit(getSetting(UNIT));
	}

	@Override
	public MovesActivitiesTask copy() {
		return copy(getClass());
	}
}
