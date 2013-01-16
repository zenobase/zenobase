package com.zenobase.tasks.fitbit;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthTask;

public class FitbitTask extends OAuthTask {

	public static final String TYPE = "fitbit";
	public static final TokenField TAG = new TokenField("tag");

	public FitbitTask(ObjectNode node) {
		super(node);
	}

	FitbitTask(String bucketId, Identity principal, Token accessToken, String marker, String tag) {
		super(TYPE, bucketId, principal, accessToken);
		setMarker(marker);
		setTag(tag);
	}

	public String getTag() {
		return getSetting(TAG);
	}

	public void setTag(String tag) {
		setSetting(TAG, tag);
	}

	@Override
	public FitbitTask copy() {
		return copy(getClass());
	}
}
