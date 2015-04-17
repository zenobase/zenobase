package com.zenobase.tasks.trakt;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;

class TraktHistoryResult {

	static final Resource SOURCE = new Resource("Trakt.tv", "https://trakt.tv/");

	private final JsonNode node;
	private final Identity author;
	private final DateTime after;
	private final DateTimeZone zone;

	public TraktHistoryResult(JsonNode node, Identity author, DateTime after, DateTimeZone zone) {
		this.node = Preconditions.checkNotNull(node);
		this.author = author;
		this.after = after;
		this.zone = zone;
	}

	public List<Event> getEvents() {
		List<Event> events = Lists.newArrayList();
		for (JsonNode activityNode : node) {
			Event event = newEvent(activityNode);
			if (event != null) {
				events.add(event);
			}
		}
		return events;
	}

	private Event newEvent(JsonNode node) {
		DateTime t = dateTimeValue(node.path("watched_at"));
		Event event = null;
		if (t.isAfter(after)) {
			event = new Event();
			event.setValue(Event.TIMESTAMP, t);
			if (!node.path("movie").isMissingNode()) {
				event.addValue(Event.TAG, "movie");
				addTags(node.path("movie"), event);
				setDuration(node.path("movie"), event);
				event.setValue(Event.RESOURCE, movieResourceValue(node.path("movie")));
			} else if (!node.path("episode").isMissingNode()) {
				event.addValue(Event.TAG, "episode");
				addTags(node.path("show"), event);
				setDuration(node.path("show"), event);
				event.setValue(Event.RESOURCE, episodeResourceValue(node.path("show"), node.path("episode")));
			} else {
				throw new RuntimeException("Expected either a movie or an episode: " + node);
			}
			event.setValue(Event.SOURCE, SOURCE);
			event.setValue(Event.AUTHOR, author);
		}
		return event;
	}

	private void addTags(JsonNode node, Event event) {
		for (JsonNode genreNode : node.path("genres")) {
			event.addValue(Event.TAG, genreNode.textValue());
		}
	}

	private void setDuration(JsonNode node, Event event) {
		event.setValue(Event.DURATION, durationValue(node.path("runtime")));
	}

	private DateTime dateTimeValue(JsonNode node) {
		Preconditions.checkState(node.isTextual(), "expected a node with a time: <%s>", node);
		return DateTime.parse(node.textValue()).withZone(zone);
	}

	private Duration durationValue(JsonNode node) {
		return node.intValue() > 0 ? Duration.standardMinutes(node.intValue()) : null;
	}

	private Resource movieResourceValue(JsonNode node) {
		String movieTitle = node.path("title").textValue();
		int year = node.path("year").intValue();
		long id = node.path("ids").path("trakt").longValue();
		Preconditions.checkNotNull(movieTitle, "Missing title: %s", node);
		Preconditions.checkState(year > 0, "Missing year: %s", node);
		Preconditions.checkState(id > 0, "Missing trakt id: %s", node);
		String title = String.format("%s (%d)", movieTitle, year);
		return new Resource(title, "https://trakt.tv/search/trakt/" + id + "?id_type=movie");
	}

	private Resource episodeResourceValue(JsonNode showNode, JsonNode episodeNode) {
		String showTitle = showNode.path("title").textValue();
		String episodeTitle = episodeNode.path("title").textValue();
		long id = episodeNode.path("ids").path("trakt").longValue();
		Preconditions.checkNotNull(showTitle, "Missing show title: %s", showNode);
		Preconditions.checkNotNull(episodeTitle, "Missing episode title: %s", episodeNode);
		Preconditions.checkState(id > 0, "Missing trakt id: %s", episodeNode);
		String title = String.format("%s: %s", showTitle, episodeTitle);
		return new Resource(title, "https://trakt.tv/search/trakt/" + id + "?id_type=episode");
	}
}
