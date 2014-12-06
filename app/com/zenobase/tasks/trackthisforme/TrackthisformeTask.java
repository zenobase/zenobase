package com.zenobase.tasks.trackthisforme;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class TrackthisformeTask extends Task {

	public static final String TYPE = "trackthisforme";
	public static final TokenField CATEGORY = new TokenField("category");
	public static final TokenField FIELD = new TokenField("field");
	public static final TokenField UNIT = new TokenField("unit");
	public static final BooleanField RATING = new BooleanField("rating");

	public TrackthisformeTask(ObjectNode node) {
		super(node);
	}

	public TrackthisformeTask(String bucketId, Identity principal, String category, String field, String unit, boolean rating, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(CATEGORY, Preconditions.checkNotNull(category));
		setSetting(FIELD, field);
		setSetting(UNIT, unit);
		setSetting(RATING, rating);
		setMarker(marker);
	}

	public String getCategory() {
		return getSetting(CATEGORY);
	}

	public String getField() {
		return getSetting(FIELD);
	}

	public String getUnit() {
		return getSetting(UNIT);
	}

	public boolean includeRatings() {
		return getSetting(RATING);
	}

	@Override
	public TrackthisformeTask copy() {
		return copy(getClass());
	}
}
