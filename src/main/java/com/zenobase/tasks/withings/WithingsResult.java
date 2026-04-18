package com.zenobase.tasks.withings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;
import java.util.List;
import org.jspecify.annotations.Nullable;

public abstract class WithingsResult {

	public static final Resource SOURCE = new Resource("Withings", "https://withings.com/");

	protected final ObjectNode node;
	protected final Identity author;
	protected final @Nullable String tag;

	public WithingsResult(ObjectNode node, Identity author, @Nullable String tag) {
		this.node = node;
		this.author = author;
		this.tag = tag;
	}

	public int getStatus() {
		return node.path("status").isInt() ? node.path("status").intValue() : -1;
	}

	public abstract @Nullable String getMarker();

	public abstract List<Event> getEvents();
}
