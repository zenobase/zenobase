package com.zenobase.tasks.fitbark;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

class DogsResult {

	private final JsonNode node;

	public DogsResult(JsonNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public List<Dog> getDogs() {
		List<Dog> dogs = new ArrayList<>();
		for (JsonNode dogNode : node.path("dog_relations")) {
			dogs.add(newDog(dogNode));
		}
		return dogs;
	}

	private Dog newDog(JsonNode node) {
		String id = node.path("dog").path("slug").textValue();
		String name = node.path("dog").path("name").textValue();
		DateTimeZone zone = DateTimeZone.forID(node.path("dog").path("tzname").textValue());
		DateTime created = DateTime.parse(node.path("date").textValue()).withZone(zone);
		DateTime modified = DateTime.parse(
						node.path("dog").path("activity_date").textValue())
				.withZoneRetainFields(zone);
		return new Dog(id, name, created, modified);
	}
}
