package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.IntNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.search.DecimalRangeConstraint;
import com.zenobase.search.TermConstraint;

public class IntegerField extends Field<Integer> {

	private final boolean indexed;

	public IntegerField(String name) {
		this(name, true);
	}

	public IntegerField(String name, boolean indexed) {
		super(name, Long.class, "integer");
		this.indexed = indexed;
		addConstraint(new DecimalRangeConstraint());
		addConstraint(new TermConstraint());
	}

	@Override
	protected Integer getValue(JsonNode node) {
		return node.getIntValue();
	}

	@Override
	public JsonNode toJson(Integer value) {
		return value != null ? new IntNode(value) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		if (!indexed) {
			schema.put("index", "no");
		}
	}
}
