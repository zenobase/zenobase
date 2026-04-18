package com.zenobase.tasks.lastfm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

class RecentTracksResult {

	static final String MUSICBRAINZ_URL = "https://musicbrainz.org/recording/";
	static final Resource SOURCE = new Resource("Last.fm", "https://www.last.fm/");

	private final ObjectNode node;
	private final Identity author;
	private final @Nullable String tag;
	private final DateTimeZone timezone;

	public RecentTracksResult(ObjectNode node, Identity author, @Nullable String tag, DateTimeZone timezone) {
		this.node = node;
		this.author = author;
		this.tag = tag;
		this.timezone = timezone;
	}

	public boolean isSuccess() {
		return node.path("error").isMissingNode();
	}

	public List<Event> getEvents() {
		List<Event> events = new ArrayList<>();
		for (JsonNode trackNode : node.path("recenttracks").path("track")) {
			addEvent(trackNode, events);
		}
		return events;
	}

	private void addEvent(JsonNode node, List<Event> events) {
		DateTime time = dateTimeValue(node.path("date").path("uts"));
		if (time != null) {
			// i.e. not "nowplaying" : "true"
			Event event = new Event();
			event.setValue(Event.TIMESTAMP, time);
			event.setValue(Event.RESOURCE, resourceValue(node));
			event.setValue(Event.AUTHOR, author);
			event.setValue(Event.SOURCE, SOURCE);
			event.addValue(Event.TAG, tag);
			events.add(event);
		}
	}

	private @Nullable DateTime dateTimeValue(JsonNode node) {
		long millis = node.asLong() * 1000;
		return millis > 0 ? new DateTime(millis, timezone) : null;
	}

	private static Resource resourceValue(JsonNode node) {
		String artist = Preconditions.checkNotNull(
			textValue(node.path("artist").path("#text")),
			"missing artist name: %s",
			node
		);
		String name = Preconditions.checkNotNull(textValue(node.path("name")), "missing recording name: %s", node);
		String mbid = Strings.emptyToNull(node.path("mbid").textValue());
		String url = mbid != null ? MUSICBRAINZ_URL + mbid : node.path("url").textValue();
		return new Resource(artist + " - " + name, url);
	}

	private static @Nullable String textValue(JsonNode node) {
		return Strings.emptyToNull(node.textValue());
	}

	public boolean hasNext() {
		JsonNode attrNode = node.path("recenttracks").path("@attr");
		int page = attrNode.path("page").asInt();
		int totalPages = attrNode.path("totalPages").asInt();
		return page < totalPages;
	}
}
