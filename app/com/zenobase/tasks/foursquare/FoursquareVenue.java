package com.zenobase.tasks.foursquare;

import java.util.List;

import com.google.common.collect.Lists;

import com.zenobase.models.Resource;

public class FoursquareVenue {

	public static final FoursquareVenue UNKNOWN = new FoursquareVenue(null, "?");

	private final String id;
	private final String name;
	private final List<String> categories = Lists.newArrayList();

	public FoursquareVenue(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public void addCategory(String category) {
		categories.add(category);
	}

	public List<String> getCategories() {
		return categories;
	}

	public Resource toResource() {
		return id != null ? new Resource(name, "https://foursquare.com/venue/" + id) : null;
	}
}
