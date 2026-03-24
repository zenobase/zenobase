package com.zenobase.tasks.mapmyfitness;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

class TypeResult {

	private final JsonNode node;

	public TypeResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public String getName() {
		String value = node.path("name").asText();
		Preconditions.checkArgument(!Strings.isNullOrEmpty(value), "Can't find activity type name: %s", node);
		return value;
	}
}
