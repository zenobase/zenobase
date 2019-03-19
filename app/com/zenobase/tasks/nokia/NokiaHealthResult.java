package com.zenobase.tasks.nokia;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

public abstract class NokiaHealthResult {

	public static final Resource SOURCE = new Resource("Withings", "https://withings.com/");

	protected final ObjectNode node;
	protected final Identity author;
	protected final String tag;

	public NokiaHealthResult(ObjectNode node, Identity author, String tag) {
		this.node = node;
		this.author = author;
		this.tag = tag;
	}

	public int getStatus() {
		return node.path("status").isInt() ? node.path("status").intValue() : -1;
	}

	public abstract List<Event> getEvents();
}
