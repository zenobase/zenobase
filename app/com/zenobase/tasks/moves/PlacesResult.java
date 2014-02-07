package com.zenobase.tasks.moves;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;

class PlacesResult {

	public static final Resource SOURCE = new Resource("Moves", "http://www.moves-app.com/");

	private final Identity author;
	private final DateTime begin;
	private final JsonNode node;

	public PlacesResult(Identity author, DateTime begin, JsonNode node) {
		this.author = Preconditions.checkNotNull(author);
		this.begin = Preconditions.checkNotNull(begin);
		this.node = Preconditions.checkNotNull(node);
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode dayNode : node) {
			addEvents((ObjectNode) dayNode, events);
		}
		return events;
	}

	public void addEvents(ObjectNode dayNode, List<Event> events) {
		for (JsonNode segmentNode : dayNode.path("segments")) {
			Event event = getEvent((ObjectNode) segmentNode);
			if (event != null) {
				events.add(event);
			}
		}
	}

	public Event getEvent(ObjectNode segmentNode) {
		Event event = null;
		DateTime begin = dateTimeValue(segmentNode.path("startTime"));
		if (!begin.isBefore(this.begin)) {
			event = new Event();
			DateTime end = dateTimeValue(segmentNode.path("endTime"));
			event.setValue(Event.TIMESTAMP, begin);
			event.setValue(Event.DURATION, new Duration(begin, end));
			event.addValue(Event.TAG, "Place");
			JsonNode placeNode = segmentNode.path("place");
			event.setValue(Event.LOCATION, locationValue(placeNode.path("location")));
			String placeType = segmentNode.path("place").path("type").textValue();
			if ("foursquare".equals(placeType)) {
				String foursquareId = placeNode.path("foursquareId").textValue();
				event.addValue(Event.RESOURCE, new Resource(foursquareId, ""));
			} else if ("home".equals(placeType) || "user".equals(placeType)) {
				String name = placeNode.path("name").textValue();
				event.addValue(Event.TAG, name);
			}
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
		}
		return event;
	}

	private static DateTime dateTimeValue(JsonNode node) {
		return DateTime.parse(node.textValue(), ISODateTimeFormat.basicDateTimeNoMillis().withOffsetParsed());
	}

	private static Location locationValue(JsonNode node) {
		return new Location(node.path("lat").decimalValue(), node.path("lon").decimalValue());
	}
}
