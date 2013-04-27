package com.zenobase.tasks.cosm;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.model.Token;

import com.zenobase.json.IntegerField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthTask;

public class CosmTask extends OAuthTask {

	public static final String TYPE = "cosm";
	public static final IntegerField FEED = new IntegerField("feed");

	public CosmTask(ObjectNode node) {
		super(node);
	}

	public CosmTask(String bucketId, Identity principal, int feedId) {
		super(TYPE, bucketId, principal);
		setCredential(TOKEN, Token.empty());
		setSetting(FEED, feedId);
	}

	CosmTask(String bucketId, Identity principal, int feedId, Token accessToken, String marker) {
		super(TYPE, bucketId, principal, accessToken);
		setSetting(FEED, feedId);
		setMarker(marker);
	}

	public int getFeedId() {
		return getSetting(FEED);
	}

	@Override
	public CosmTask copy() {
		return copy(getClass());
	}
}
