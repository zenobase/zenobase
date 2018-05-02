package com.zenobase.tasks.jawbone;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class JawboneSleepTask extends Task {

	public static final String TYPE = "jawbone-sleep";
	public static final TokenField TAG = new TokenField("tag");
	public static final BooleanField RANGES = new BooleanField("ranges");

	public JawboneSleepTask(ObjectNode node) {
		super(node);
	}

	public JawboneSleepTask(String bucketId, Identity principal, String tag) {
		this(bucketId, principal, tag, null);
	}

	JawboneSleepTask(String bucketId, Identity principal, String tag, String marker) {
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
	public JawboneSleepTask copy() {
		return copy(getClass());
	}
}
