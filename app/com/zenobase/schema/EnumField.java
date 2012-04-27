package com.zenobase.schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

public class EnumField<E extends Enum<E>> extends Field<E> {

	public EnumField(String name, Class<E> type) {
		super(name, type, "string");
	}

	@Override
	protected E getValue(JsonNode node) {
		return Enum.valueOf((Class<E>) getType(), node.getTextValue());
	}

	@Override
	protected JsonNode toJson(E value) {
		return new TextNode(value.toString());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "not_analyzed");
	}
}
