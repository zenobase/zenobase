package com.zenobase.tasks.foursquare;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.zenobase.models.Resource;

public class FoursquareVenue {

	public static final FoursquareVenue UNKNOWN = new FoursquareVenue(null, "?");

	private final @Nullable String id;
	private final String name;
	private final List<String> categories = new ArrayList<>();

	public FoursquareVenue(@Nullable String id, String name) {
		this.id = id;
		this.name = name;
	}

	public void addCategory(String category) {
		categories.add(category);
	}

	public List<String> getCategories() {
		return categories;
	}

	public @Nullable Resource toResource() {
		return id != null ? new Resource(name, "https://foursquare.com/venue/" + id) : null;
	}
}
