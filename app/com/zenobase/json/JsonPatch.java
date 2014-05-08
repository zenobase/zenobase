package com.zenobase.json;

import java.util.Iterator;
import java.util.Map;

import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class JsonPatch {

	private final ObjectNode from, to;

	public JsonPatch(ObjectNode from, ObjectNode to) {
		this.from = from;
		this.to = to;
	}

	public ObjectNode apply(ObjectNode node) {
		checkState(node, from);
		ObjectNode patched = node.deepCopy();
		apply(patched, to);
		return patched;
	}

	private void checkState(ObjectNode node, ObjectNode expected) throws IllegalStateException {
		for (Iterator<Map.Entry<String, JsonNode>> i = expected.fields(); i.hasNext();) {
			Map.Entry<String, JsonNode> entry = i.next();
			JsonNode found = node.path(entry.getKey());
			if (entry.getValue().isNull()) {
				Preconditions.checkState(found.isMissingNode(),
					"Expected value of field <%s> to be empty but found <%s>", entry.getKey(), found);
			} else if (entry.getValue().isValueNode()) {
				if (!entry.getValue().equals(found)) {
					entry.setValue(found); // TODO
					Logger.warn(String.format("Expected value of field <%s> to be <%s> but found <%s>", entry.getKey(), entry.getValue(), found));
					// Logger.warn("Object: " + node);
					// Logger.warn("From:   " + from);
					// Logger.warn("To:     " + to);
				}
				// Preconditions.checkState(entry.getValue().equals(found),
				//	"Expected value of field <%s> to be <%s> but found <%s>", entry.getKey(), entry.getValue(), found);
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
		for (Iterator<Map.Entry<String, JsonNode>> i = changes.fields(); i.hasNext();) {
			Map.Entry<String, JsonNode> entry = i.next();
			if (entry.getValue().isNull()) {
				target.remove(entry.getKey());
			} else if (entry.getValue().isValueNode()) {
				target.put(entry.getKey(), entry.getValue());
			} else if (target.path(entry.getKey()).isValueNode()) {
				target.put(entry.getKey(), entry.getValue());
			} else if (entry.getValue().isObject()) {
				apply(target.with(entry.getKey()), (ObjectNode) entry.getValue());
			} else {
				throw new AssertionError();
			}
		}
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("from", from).add("to", to).toString();
	}
}
