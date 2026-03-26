package com.zenobase.tasks.oura;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class OuraSleepTask extends Task {

	public static final String TYPE = "oura-sleep";
	public static final TokenField TAG = new TokenField("tag");

	public OuraSleepTask(ObjectNode node) {
		super(node);
	}

	public OuraSleepTask(String bucketId, Identity principal, String marker, String tag) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
	}

	public @Nullable DateTime getBegin() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	@Override
	public OuraSleepTask copy() {
		return copy(getClass());
	}
}
