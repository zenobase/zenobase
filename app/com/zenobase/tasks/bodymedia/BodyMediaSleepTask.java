package com.zenobase.tasks.bodymedia;

import org.elasticsearch.common.base.Objects;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class BodyMediaSleepTask extends Task {

	public static final String TYPE = "bodymedia-sleep";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField RANGES = new BooleanField("ranges");

	public BodyMediaSleepTask(ObjectNode node) {
		super(node);
	}

	public BodyMediaSleepTask(String bucketId, Identity principal, String tag, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(TAG, tag);
		setSetting(RANGES, true);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public boolean useRanges() {
		return Objects.firstNonNull(getSetting(RANGES), false);
	}

	@Override
	public BodyMediaSleepTask copy() {
		return copy(getClass());
	}
}
