package com.zenobase.tasks.lastfm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.joda.time.Duration;

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
		TrackInfo track = new TrackInfo(resource, duration);
		for (JsonNode tagNode : node.path("track").path("toptags").path("tag")) {
			String tag = tagNode.path("name").textValue();
			if (tag != null) {
				track.addTag(tag);
			}
		}
		return track;
	}

	private static Resource resourceValue(JsonNode node) {
		String artistName = textValue(node.path("artist").path("name"));
		String trackName = textValue(node.path("name"));
		String url = textValue(node.path("url"));
		Preconditions.checkNotNull(artistName, "missing artist name: %s", node);
		Preconditions.checkNotNull(trackName, "missing track name: %s", node);
		return new Resource(artistName + " - " + trackName, url);
	}

	private static String textValue(JsonNode node) {
		return Strings.emptyToNull(node.textValue());
	}

	private static Duration durationValue(JsonNode node) {
		long duration = node.asLong();
		return duration > 0 ? Duration.millis(duration) : null;
	}
}
