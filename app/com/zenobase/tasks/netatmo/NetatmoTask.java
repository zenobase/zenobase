package com.zenobase.tasks.netatmo;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class NetatmoTask extends Task {

	public static final String TYPE = "netatmo";

	public NetatmoTask(ObjectNode node) {
		super(node);
	}

	public NetatmoTask(String bucketId, Identity principal) {
		this(bucketId, principal, null);
	}

	NetatmoTask(String bucketId, Identity principal, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public NetatmoTask copy() {
		return copy(getClass());
	}
}
