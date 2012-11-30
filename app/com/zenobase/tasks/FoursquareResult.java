package com.zenobase.tasks;

import java.math.BigDecimal;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;

class FoursquareResult {

	public static final Resource SOURCE = new Resource("Foursquare", "http://foursquare.com/");

	private final Identity author;
	private final JsonNode node;

	public FoursquareResult(Identity author, JsonNode node) {
		this.author = author;
		this.node = node;
	}

	public int getStatus() {
		return node.path("meta").path("code").getIntValue();
	}

	public int getTotal() {
		return node.path("response").path("checkins").path("count").getIntValue();
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode item : node.path("response").path("checkins").path("items")) {
			events.add(getCheckin(item).getEvent());
		}
		return events;
	}

	private Checkin getCheckin(JsonNode item) {
		return new Checkin(item, author);
	}

	static class Checkin {

		private final JsonNode node;
		private final Identity author;

		public Checkin(JsonNode node, Identity author) {
			this.node = node;
			this.author = author;
		}

		public Event getEvent() {
			Event event = new Event();
			long time = node.get("createdAt").getLongValue() * 1000;
			int offset = node.get("timeZoneOffset").getIntValue() * 60 * 1000;
			event.setValue(Event.TIMESTAMP, new DateTime(time, DateTimeZone.forOffsetMillis(offset)));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			Venue venue = getVenue();
			event.setValue(Event.RESOURCE, venue.getResource());
			event.setValue(Event.LOCATION, venue.getLocation());
			for (String tag : venue.getTags()) {
				event.addValue(Event.TAG, tag);
			}
			return event;
		}

		private Venue getVenue() {
			return new Venue(node.path("venue"));
		}
	}

	static class Venue {

		private final JsonNode node;

		public Venue(JsonNode node) {
			this.node = node;
		}

		public Resource getResource() {
			String title = node.get("name").getTextValue();
			String url = Objects.firstNonNull(node.path("url").getTextValue(), SOURCE.getUrl());
			return title != null ? new Resource(title, url) : null;
		}

		public Location getLocation() {
			return getLocation(node.path("location"));
		}

		private static Location getLocation(JsonNode node) {
			BigDecimal lat = node.path("lat").getDecimalValue();
			BigDecimal lon = node.path("lng").getDecimalValue();
			return lat != BigDecimal.ZERO && lon != BigDecimal.ZERO ? new Location(lat, lon) : null;
		}

		public List<String> getTags() {
			List<String> tags = Lists.newArrayList();
			for (JsonNode category : node.path("categories")) {
				tags.add(category.get("name").getTextValue());
			}
			return tags;
		}
	}
}
