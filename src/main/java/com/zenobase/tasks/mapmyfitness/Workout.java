package com.zenobase.tasks.mapmyfitness;

import org.jspecify.annotations.Nullable;

import com.zenobase.models.Event;
import com.zenobase.models.Location;

public record Workout(
		Event event, @Nullable String typeId, @Nullable String routeId) {

	public void addTag(String tag) {
		event.addValue(Event.TAG, tag);
	}

	public void setLocation(Location location) {
		event.setValue(Event.LOCATION, location);
	}
}
