package com.zenobase.tasks;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FoursquareCheckinsNode {

	private final ObjectNode node;
	private final Identity author;

	public FoursquareCheckinsNode(ObjectNode node, Identity author) {
		this.node = node;
		this.author = author;
		Preconditions.checkState(node.path("meta").path("code").getIntValue() == 200);
		Preconditions.checkState(node.path("response").path("checkins").path("items").getIntValue() <= 100); // TODO loop if necessary
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode item : node.path("response").path("checkins").path("items")) {
			events.add(getCheckin(item).getEvent());
		}
		return events;
	}

	private FoursquareCheckinNode getCheckin(JsonNode item) {
		return new FoursquareCheckinNode((ObjectNode) item, author);
	}
}
