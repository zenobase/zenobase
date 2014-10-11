package com.zenobase.tasks.netatmo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class NetatmoTask extends Task {

	public static final String TYPE = "netatmo";
	public static final BooleanField MODULES = new BooleanField("modules");

	public NetatmoTask(ObjectNode node) {
		super(node);
	}

	public NetatmoTask(String bucketId, Identity principal, boolean includeModules) {
		this(bucketId, principal, includeModules, null);
	}

	NetatmoTask(String bucketId, Identity principal, boolean includeModules, String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(MODULES, includeModules);
	}

	public boolean includeModules() {
		return Objects.firstNonNull(getSetting(MODULES), Boolean.FALSE);
	}

	@Override
	public NetatmoTask copy() {
		return copy(getClass());
	}
}
