package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Event;

class FoursquareCheckinNode {

	private final ObjectNode node;

	public FoursquareCheckinNode(ObjectNode node) {
		this.node = node;
	}

	public Event getEvent() {
		Event event = new Event();
		long time = node.get("createdAt").getLongValue() * 1000;
		int offset = node.get("timeZoneOffset").getIntValue() * 60 * 1000;
		event.addValue(Event.TIMESTAMP, new DateTime(time, DateTimeZone.forOffsetMillis(offset)));
		FoursquareVenueNode venue = getVenue();
		event.addValue(Event.RESOURCE, venue.getResource());
		event.addValue(Event.LOCATION, venue.getLocation());
		for (String tag : venue.getTags()) {
			event.addValue(Event.TAG, tag);
		}
		return event;
	}

	private FoursquareVenueNode getVenue() {
		return new FoursquareVenueNode((ObjectNode) node.path("venue"));
	}
}
