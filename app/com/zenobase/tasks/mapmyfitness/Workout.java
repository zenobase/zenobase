package com.zenobase.tasks.mapmyfitness;

import com.zenobase.models.Event;
import com.zenobase.models.Location;

public class Workout {

	private final Event event;
	private final String typeId;
	private final String routeId;

	public Workout(Event event, String typeId, String routeId) {
		this.event = event;
		this.typeId = typeId;
		this.routeId = routeId;
	}

	public Event getEvent() {
		return event;
	}

	public String getTypeId() {
		return typeId;
	}

	public void addTag(String tag) {
		event.addValue(Event.TAG, tag);
	}

	public String getRouteId() {
		return routeId;
	}

	public void setLocation(Location location) {
		event.setValue(Event.LOCATION, location);
	}
}
