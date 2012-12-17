package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

public class EnumField<E extends Enum<E>> extends Field<E> {

	public static <T extends Enum<T>> EnumField<T> newInstance(String name, Class<T> type) {
		return new EnumField<T>(name, type);
	}

	private EnumField(String name, Class<E> type) {
		super(name, type, "string");
	}

	@Override
	protected E getValue(JsonNode node) {
		String value = node.getTextValue();
		return value != null ? Enum.valueOf((Class<E>) getType(), value.toUpperCase()) : null;
	}

	@Override
	public JsonNode toJson(E value) {
		return value != null ? new TextNode(value.toString().toLowerCase()) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "not_analyzed");
	}
}
