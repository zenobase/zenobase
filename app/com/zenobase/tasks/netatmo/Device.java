package com.zenobase.tasks.netatmo;

import org.joda.time.DateTime;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import com.zenobase.models.Location;

public class Device {

	private final String id;
	private final String moduleId;
	private final String label;
	private final DateTime created;
	private final DateTime updated;
	private final Location location;

	public Device(String id, String label, DateTime created, DateTime updated, Location location) {
		this(id, null, label, created, updated, location);
	}

	public Device(String id, String moduleId, String label, DateTime created, DateTime updated, Location location) {
		this.id = Preconditions.checkNotNull(id);
		this.moduleId = moduleId;
		this.label = Preconditions.checkNotNull(label);
		this.created = Preconditions.checkNotNull(created);
		this.updated = Preconditions.checkNotNull(updated);
		this.location = Preconditions.checkNotNull(location);
	}

	public String getId() {
		return id;
	}

	public String getModuleId() {
		return moduleId;
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
