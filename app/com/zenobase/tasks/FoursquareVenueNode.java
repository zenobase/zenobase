package com.zenobase.tasks;

import java.math.BigDecimal;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.zenobase.models.Location;
import com.zenobase.models.Resource;

class FoursquareVenueNode {

	private final ObjectNode node;

	public FoursquareVenueNode(ObjectNode node) {
		this.node = node;
	}

	public Resource getResource() {
		String title = node.get("name").getTextValue();
		String url = Objects.firstNonNull(node.path("url").getTextValue(), "http://foursquare.com/"); // TODO link to query?
		return new Resource(title, url);
	}

	public Location getLocation() {
		BigDecimal lat = node.path("location").path("lat").getDecimalValue();
		BigDecimal lon = node.path("location").path("lng").getDecimalValue();
		return new Location(lat, lon);
	}

	public List<String> getTags() {
		List<String> tags = Lists.newArrayList();
		for (JsonNode category : node.path("categories")) {
			tags.add(category.get("name").getTextValue());
		}
		return tags;
	}
}
