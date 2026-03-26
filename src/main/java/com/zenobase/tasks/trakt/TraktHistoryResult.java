package com.zenobase.tasks.trakt;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

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
		List<Event> events = new ArrayList<>();
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
		String title = node.path("title").textValue();
		int year = node.path("year").intValue();
		long id = node.path("ids").path("trakt").longValue();
		Preconditions.checkNotNull(title, "Missing title: %s", node);
		Preconditions.checkState(id > 0, "Missing trakt id: %s", node);
		if (year > 0) {
			title = String.format("%s (%d)", title, year);
		}
		return new Resource(title, "https://trakt.tv/search/trakt/" + id + "?id_type=movie");
	}

	private Resource episodeResourceValue(JsonNode showNode, JsonNode episodeNode) {
		String title = showNode.path("title").textValue();
		Preconditions.checkNotNull(title, "Missing show title: %s", showNode);
		String episodeTitle = episodeNode.path("title").textValue();
		if (episodeTitle != null) {
			title += ": " + episodeTitle;
		}
		int season = episodeNode.path("season").intValue();
		int number = episodeNode.path("number").intValue();
		if (season * number > 0) {
			title += String.format(" (Season %d, Episode %d)", season, number);
		}
		long id = episodeNode.path("ids").path("trakt").longValue();
		Preconditions.checkState(id > 0, "Missing trakt id: %s", episodeNode);
		return new Resource(title, "https://trakt.tv/search/trakt/" + id + "?id_type=episode");
	}
}
