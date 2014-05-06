package com.zenobase.tasks.automatic;

import org.joda.time.DateTime;

import com.zenobase.models.Event;

public class Trip {

	private final Event event;
	private final String vehicleId;

	public Trip(Event event, String vehicleId) {
		this.event = event;
		this.vehicleId = vehicleId;
	}

	public boolean isAfter(DateTime time) {
		return event.getValue(Event.TIMESTAMP).isAfter(time);
	}

	public Event getEvent() {
		return event;
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void addTag(String tag) {
		event.addValue(Event.TAG, tag);
	}
}
