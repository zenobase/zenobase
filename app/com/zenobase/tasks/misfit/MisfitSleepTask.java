package com.zenobase.tasks.misfit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class MisfitSleepTask extends Task {

	public static final String TYPE = "misfit-sleep";
	public static final TokenField TAG = new TokenField("tag");

	public MisfitSleepTask(ObjectNode node) {
		super(node);
	}

	public MisfitSleepTask(String bucketId, Identity principal, String tag, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setMarker(marker);
	}

	public DateTime getBegin() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public String getTag() {
		return getSetting(TAG);
	}

	@Override
	public MisfitSleepTask copy() {
		return copy(getClass());
	}
}
