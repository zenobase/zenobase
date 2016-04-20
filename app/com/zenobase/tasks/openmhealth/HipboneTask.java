package com.zenobase.tasks.openmhealth;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class HipboneTask extends Task {

	public static final String TYPE = "hipbone";
	public static final TokenField FOLDER = new TokenField("folder");

	public HipboneTask(ObjectNode node) {
		super(node);
	}

	public HipboneTask(String bucketId, Identity principal) {
		super(TYPE, bucketId, principal);
	}

	HipboneTask(String bucketId, Identity principal, String folder) {
		super(TYPE, bucketId, principal);
		setSetting(FOLDER, folder);
	}

	public String getFolder() {
		return getSetting(FOLDER);
	}

	@Override
	public HipboneTask copy() {
		return copy(getClass());
	}
}
