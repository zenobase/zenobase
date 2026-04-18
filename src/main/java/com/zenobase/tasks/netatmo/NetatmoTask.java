package com.zenobase.tasks.netatmo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.json.BooleanField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import org.jspecify.annotations.Nullable;

public class NetatmoTask extends Task {

	public static final String TYPE = "netatmo";
	public static final BooleanField MODULES = new BooleanField("modules");
	public static final BooleanField HOURLY = new BooleanField("hourly");

	public NetatmoTask(ObjectNode node) {
		super(node);
	}

	public NetatmoTask(String bucketId, Identity principal, boolean includeModules, boolean hourly) {
		this(bucketId, principal, includeModules, hourly, null);
	}

	NetatmoTask(String bucketId, Identity principal, boolean includeModules, boolean hourly, @Nullable String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
		setSetting(MODULES, includeModules);
		setSetting(HOURLY, hourly);
	}

	public boolean includeModules() {
		return MoreObjects.firstNonNull(getSetting(MODULES), Boolean.FALSE);
	}

	public boolean isHourly() {
		return MoreObjects.firstNonNull(getSetting(HOURLY), false);
	}

	@Override
	public NetatmoTask copy() {
		return copy(getClass());
	}
}
