package com.zenobase.json;

import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Preconditions;

public class JsonPatch {

	private final ObjectNode from, to;

	public JsonPatch(ObjectNode from, ObjectNode to) {
		this.from = from;
		this.to = to;
	}

	public ObjectNode apply(ObjectNode node) {
		Preconditions.checkNotNull(from); // TODO check existing values
		ObjectNode patched = Nodes.copy(node);
		apply(patched, to);
		return patched;
	}

	private static void apply(ObjectNode target, ObjectNode changes) {
		for (Iterator<Map.Entry<String, JsonNode>> i = changes.getFields(); i.hasNext();) {
			Map.Entry<String, JsonNode> entry = i.next();
			if (entry.getValue().isNull()) {
				target.remove(entry.getKey());
			} else if (entry.getValue().isValueNode()) {
				target.put(entry.getKey(), entry.getValue());
			} else if (entry.getValue().isObject()) {
				apply(target.with(entry.getKey()), (ObjectNode) entry.getValue());
			}
		}
	}
}
