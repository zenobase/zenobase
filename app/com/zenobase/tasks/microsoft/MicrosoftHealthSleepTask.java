package com.zenobase.tasks.microsoft;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class MicrosoftHealthSleepTask extends MicrosoftHealthTaskSupport {

	public static final String TYPE = "microsoft-sleep";
	public static final TokenField TAG = new TokenField("tag");

	public MicrosoftHealthSleepTask(ObjectNode node) {
		super(node);
	}

	MicrosoftHealthSleepTask(String bucketId, Identity principal, DateTimeZone zone, String tag, DateTime marker) {
		super(TYPE, bucketId, principal, zone, marker);
		setSetting(TAG, tag);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	@Override
	public MicrosoftHealthSleepTask copy() {
		return copy(getClass());
	}
}
