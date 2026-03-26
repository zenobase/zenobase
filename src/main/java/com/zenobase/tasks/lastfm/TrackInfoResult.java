package com.zenobase.tasks.lastfm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Resource;

class TrackInfoResult {

	private final JsonNode node;

	public TrackInfoResult(JsonNode node) {
		this.node = node;
	}

	public boolean isSuccess() {
		return node.isObject() && node.path("error").isMissingNode();
	}

	public boolean isNotFound() {
		return node.path("error").intValue() == 6;
	}

	public TrackInfo get() {
		Resource resource = resourceValue(node.path("track"));
		Duration duration = durationValue(node.path("track").path("duration"));
		var track = new TrackInfo(resource, duration);
		for (JsonNode tagNode : node.path("track").path("toptags").path("tag")) {
			String tag = tagNode.path("name").textValue();
			if (tag != null) {
				track.addTag(tag);
			}
		}
		return track;
	}

	private static Resource resourceValue(JsonNode node) {
		String artistName = Preconditions.checkNotNull(
				textValue(node.path("artist").path("name")), "missing artist name: %s", node);
		String trackName = Preconditions.checkNotNull(textValue(node.path("name")), "missing track name: %s", node);
		String url = Preconditions.checkNotNull(textValue(node.path("url")), "missing url: %s", node);
		return new Resource(artistName + " - " + trackName, url);
	}

	private static @Nullable String textValue(JsonNode node) {
		return Strings.emptyToNull(node.textValue());
	}

	private static @Nullable Duration durationValue(JsonNode node) {
		long duration = node.asLong();
		return duration > 0 ? Duration.millis(duration) : null;
	}
}
