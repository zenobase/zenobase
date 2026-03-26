package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

public class ObjectField extends Field<ObjectNode> {

	public ObjectField(String name) {
		super(name, ObjectNode.class, "object");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("enabled", false);
	}

	@Override
	protected ObjectNode getValue(JsonNode node) {
		return (ObjectNode) node;
	}

	@Override
	public JsonNode toJson(@Nullable ObjectNode value) {
		return value != null ? value : NullNode.getInstance();
	}
}
