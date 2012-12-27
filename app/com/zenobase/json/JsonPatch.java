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
		checkState(node, from);
		ObjectNode patched = Nodes.copy(node);
		apply(patched, to);
		return patched;
	}

	private void checkState(ObjectNode node, ObjectNode expected) throws IllegalStateException {
		for (Iterator<Map.Entry<String, JsonNode>> i = expected.getFields(); i.hasNext();) {
			Map.Entry<String, JsonNode> entry = i.next();
			JsonNode found = node.path(entry.getKey());
			if (entry.getValue().isValueNode()) {
				Preconditions.checkState(entry.getValue().equals(found),
					"Expected value of field <%s> to be <%s> but found <%s>", entry.getKey(), entry.getValue(), found);
			} else if (entry.getValue().isObject()) {
				Preconditions.checkState(found.isObject(),
					"Expected value of field <%s> to be an object node but found <%s>", entry.getKey(), found);
				checkState((ObjectNode) found, (ObjectNode) entry.getValue());
			} else {
				throw new AssertionError();
			}
		}
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
			} else {
				throw new AssertionError();
			}
		}
	}
}
