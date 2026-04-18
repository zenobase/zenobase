package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Map;

public class JsonDiff {

	private final ObjectNode from = Nodes.newObject();
	private final ObjectNode to = Nodes.newObject();

	public JsonPatch diff(JsonNode original, JsonNode modified) {
		addedOrModified(original, modified);
		return new JsonPatch(from, to);
	}

	private void add(String path, JsonDiff diff) {
		from.set(path, diff.from);
		to.set(path, diff.to);
	}

	private void addedOrModified(JsonNode original, JsonNode modified) {
		for (Map.Entry<String, JsonNode> entry : modified.properties()) {
			JsonNode value = original.path(entry.getKey());
			if (value.isMissingNode()) {
				from.set(entry.getKey(), NullNode.getInstance());
				to.set(entry.getKey(), entry.getValue());
			} else if (value.isObject() && entry.getValue().isObject()) {
				JsonDiff diff = new JsonDiff();
				diff.diff(original.get(entry.getKey()), entry.getValue());
				add(entry.getKey(), diff);
			} else if (!value.equals(entry.getValue())) {
				from.set(entry.getKey(), original.get(entry.getKey()));
				to.set(entry.getKey(), entry.getValue());
			} else {
				Preconditions.checkState(
					value.equals(entry.getValue()),
					"Expected <%s> but found <%s> in field <%s>",
					entry.getValue(),
					value,
					entry.getKey()
				);
			}
		}
		for (String removedField : Sets.difference(
			Sets.newHashSet(original.fieldNames()),
			Sets.newHashSet(modified.fieldNames())
		)) {
			from.set(removedField, original.get(removedField));
			to.set(removedField, NullNode.getInstance());
		}
	}
}
