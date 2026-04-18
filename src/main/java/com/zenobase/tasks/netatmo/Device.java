package com.zenobase.tasks.netatmo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Location;

public class Device {

	private final String id;
	private final @Nullable String moduleId;
	private final String label;
	private final DateTime created;
	private final DateTime updated;
	private final Location location;
	private final ImmutableSet<String> types;

	public Device(
		String id,
		String label,
		DateTime created,
		DateTime updated,
		Location location,
		Iterable<String> types
	) {
		this(id, null, label, created, updated, location, types);
	}

	public Device(
		String id,
		@Nullable String moduleId,
		String label,
		DateTime created,
		DateTime updated,
		Location location,
		Iterable<String> types
	) {
		this.id = Preconditions.checkNotNull(id);
		this.moduleId = moduleId;
		this.label = Preconditions.checkNotNull(label);
		this.created = Preconditions.checkNotNull(created);
		this.updated = Preconditions.checkNotNull(updated);
		this.location = Preconditions.checkNotNull(location);
		this.types = ImmutableSet.copyOf(types);
	}

	public String getId() {
		return id;
	}

	public @Nullable String getModuleId() {
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

	public boolean supports(String type) {
		return types.contains(type);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("id", id)
			.add("label", label)
			.add("timestamp", updated)
			.add("location", location)
			.toString();
	}
}
