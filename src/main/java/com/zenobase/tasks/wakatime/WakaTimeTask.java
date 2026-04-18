package com.zenobase.tasks.wakatime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;

public class WakaTimeTask extends Task {

	public static final String TYPE = "wakatime";
	public static final TokenField TAG = new TokenField("tag");

	public WakaTimeTask(ObjectNode node) {
		super(node);
	}

	public WakaTimeTask(String bucketId, Identity principal, @Nullable String tag, String marker) {
		super(TYPE, bucketId, principal);
		setSetting(TAG, tag);
		setMarker(marker);
	}

	public @Nullable DateTime getBegin() {
		String marker = getMarker();
		return marker != null ? DateTime.parse(marker) : null;
	}

	public @Nullable String getTag() {
		return getSetting(TAG);
	}

	@Override
	public WakaTimeTask copy() {
		return copy(getClass());
	}
}
