package com.zenobase.tasks.trakt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import org.jspecify.annotations.Nullable;

public class TraktTask extends Task {

	public static final String TYPE = "trakt";

	public TraktTask(ObjectNode node) {
		super(node);
	}

	public TraktTask(String bucketId, Identity principal) {
		this(bucketId, principal, null);
	}

	TraktTask(String bucketId, Identity principal, @Nullable String marker) {
		super(TYPE, bucketId, principal);
		setMarker(marker);
	}

	@Override
	public TraktTask copy() {
		return copy(getClass());
	}
}
