package com.zenobase.tasks.foursquare;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Location;
import com.zenobase.models.Resource;

class FoursquareResult {

	public static final Resource SOURCE = new Resource("Foursquare", "https://foursquare.com/");

	private final Identity author;
	private final JsonNode node;

	public FoursquareResult(Identity author, JsonNode node) {
		this.author = author;
		this.node = node;
	}

	public int getStatus() {
		return node.path("meta").path("code").intValue();
	}

	public int getTotal() {
		return node.path("response").path("checkins").path("count").intValue();
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
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
			long time = node.get("createdAt").longValue() * 1000;
			int offset = node.get("timeZoneOffset").intValue() * 60 * 1000;
			event.setValue(Event.TIMESTAMP, new DateTime(time, DateTimeZone.forOffsetMillis(offset)));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			event.setValue(Event.NOTE, node.path("shout").textValue());
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

		public @Nullable Resource getResource() {
			String title = node.path("name").textValue();
			String url = MoreObjects.firstNonNull(node.path("url").textValue(), SOURCE.url());
			return title != null ? new Resource(title, url) : null;
		}

		public @Nullable Location getLocation() {
			return getLocation(node.path("location"));
		}

		private static @Nullable Location getLocation(JsonNode node) {
			BigDecimal lat = node.path("lat").decimalValue();
			BigDecimal lon = node.path("lng").decimalValue();
			return BigDecimal.ZERO.compareTo(lat) != 0 && BigDecimal.ZERO.compareTo(lon) != 0
				? new Location(lat, lon)
				: null;
		}

		public List<String> getTags() {
			List<String> tags = new ArrayList<>();
			for (JsonNode category : node.path("categories")) {
				tags.add(category.get("name").textValue());
			}
			return tags;
		}
	}
}
