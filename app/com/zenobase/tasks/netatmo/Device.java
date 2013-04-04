package com.zenobase.tasks.netatmo;

import org.joda.time.DateTime;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import com.zenobase.models.Location;

public class Device {

	private final String id;
	private final String label;
	private final DateTime created;
	private final DateTime updated;
	private final Location location;

	public Device(String id, String label, DateTime created, DateTime updated, Location location) {
		Preconditions.checkNotNull(id);
		Preconditions.checkNotNull(label);
		Preconditions.checkNotNull(created);
		Preconditions.checkNotNull(updated);
		Preconditions.checkNotNull(location);
		this.id = id;
		this.label = label;
		this.created = created;
		this.updated = updated;
		this.location = location;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public DateTime getCreated() {
		return created;
	}

	public DateTime getUpdated() {
		return updated;
	}

	public Location getLocation() {
		return location;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("id", id)
			.add("label", label)
			.add("timestamp", updated)
			.add("location", location)
			.toString();
	}
}
