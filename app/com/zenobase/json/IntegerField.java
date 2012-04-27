package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.IntNode;
import org.codehaus.jackson.node.ObjectNode;

public class IntegerField extends Field<Integer> {

	private final boolean indexed;

	public IntegerField(String name) {
		this(name, true);
	}

	public IntegerField(String name, boolean indexed) {
		super(name, Long.class, "integer");
		this.indexed = indexed;
	}

	@Override
	protected Integer getValue(JsonNode node) {
		return node.getIntValue();
	}

	@Override
	protected JsonNode toJson(Integer value) {
		return new IntNode(value);
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		if (!indexed) {
			schema.put("index", "no");
		}
	}
}
