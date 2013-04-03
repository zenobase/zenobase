package com.zenobase.tasks.netatmo;

import org.joda.time.DateTime;
import com.google.common.base.Objects;

import com.zenobase.models.Location;

public class Device {

	private final String id;
	private final String label;
	private final DateTime timestamp;
	private final Location location;

	public Device(String id, String label, DateTime timestamp, Location location) {
		this.id = id;
		this.label = label;
		this.timestamp = timestamp;
		this.location = location;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public DateTime getTimestamp() {
		return timestamp;
	}

	public Location getLocation() {
		return location;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("id", id)
			.add("label", label)
			.add("timestamp", timestamp)
			.add("location", location)
			.toString();
	}
}
