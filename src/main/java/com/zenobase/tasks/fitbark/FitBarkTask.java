package com.zenobase.tasks.fitbark;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class FitBarkTask extends Task {

	public static final String TYPE = "fitbark";
	public static final TokenField NAME = new TokenField("name");
	public static final BooleanField HOURLY = new BooleanField("hourly");

	public FitBarkTask(ObjectNode node) {
		super(node);
	}

	public FitBarkTask(String bucketId, Identity principal, String name, boolean hourly) {
		this(bucketId, principal, name, hourly, null);
	}

	FitBarkTask(String bucketId, Identity principal, String name, boolean hourly, @Nullable String marker) {
		super(TYPE, bucketId, principal);
		setSetting(NAME, name);
		setSetting(HOURLY, hourly);
		setMarker(marker);
	}

	public @Nullable String getName() {
		return getSetting(NAME);
	}

	public boolean isHourly() {
		return Objects.requireNonNull(getSetting(HOURLY));
	}

	@Override
	public FitBarkTask copy() {
		return copy(getClass());
	}
}
