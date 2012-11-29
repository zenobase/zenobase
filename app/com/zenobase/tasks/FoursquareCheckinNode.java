package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;

class FoursquareCheckinNode {

	private final ObjectNode node;
	private final Identity author;

	public FoursquareCheckinNode(ObjectNode node, Identity author) {
		this.node = node;
		this.author = author;
	}

	public Event getEvent() {
		Event event = new Event();
		long time = node.get("createdAt").getLongValue() * 1000;
		int offset = node.get("timeZoneOffset").getIntValue() * 60 * 1000;
		event.setValue(Event.TIMESTAMP, new DateTime(time, DateTimeZone.forOffsetMillis(offset)));
		event.setValue(Event.AUTHOR, author);
		FoursquareVenueNode venue = getVenue();
		event.setValue(Event.RESOURCE, venue.getResource());
		event.setValue(Event.LOCATION, venue.getLocation());
		for (String tag : venue.getTags()) {
			event.addValue(Event.TAG, tag);
		}
		return event;
	}

	private FoursquareVenueNode getVenue() {
		return new FoursquareVenueNode((ObjectNode) node.path("venue"));
	}
}
