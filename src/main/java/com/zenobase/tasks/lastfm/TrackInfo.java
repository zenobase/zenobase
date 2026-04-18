package com.zenobase.tasks.lastfm;

import com.google.common.collect.ImmutableList;
import com.zenobase.models.Event;
import com.zenobase.models.Resource;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;

class TrackInfo {

	private final Resource resource;
	private final @Nullable Duration duration;
	private final List<String> tags = new ArrayList<>();

	public TrackInfo(Resource resource, @Nullable Duration duration) {
		this.resource = resource;
		this.duration = duration;
	}

	public Resource getResource() {
		return resource;
	}

	public @Nullable Duration getDuration() {
		return duration;
	}

	public void addTag(String tag) {
		tags.add(tag);
	}

	public ImmutableList<String> getTags() {
		return ImmutableList.copyOf(tags);
	}

	public void apply(Event event) {
		event.setValue(Event.RESOURCE, resource);
		event.setValue(Event.DURATION, duration);
		for (String tag : tags) {
			event.addValue(Event.TAG, tag);
		}
	}
}
