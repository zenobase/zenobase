package com.zenobase.tasks.trackthisforme;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

class TrackthisformeCategoriesResult {

	private final JsonNode node;

	public TrackthisformeCategoriesResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public Category getCategory(String name) {
		for (JsonNode categoryNode : node.path("categories")) {
			if (name.equalsIgnoreCase(categoryNode.path("name").textValue())) {
				String id = categoryNode.path("id").asText();
				String unit = Strings.emptyToNull(categoryNode.path("symbol").textValue());
				return new Category(id, name, unit);
			}
		}
		return null;
	}
}
