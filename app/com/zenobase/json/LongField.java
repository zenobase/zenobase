package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.LongNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.search.DecimalRangeConstraintBuilder;
import com.zenobase.search.TermConstraintBuilder;

public class LongField extends Field<Long> {

	private final boolean indexed;

	public LongField(String name) {
		this(name, true);
	}

	public LongField(String name, boolean indexed) {
		super(name, Long.class, "long");
		this.indexed = indexed;
		if (indexed) {
			addConstraint(new DecimalRangeConstraintBuilder());
			addConstraint(new TermConstraintBuilder());
		}
	}

	@Override
	protected Long getValue(JsonNode node) {
		return node.getLongValue();
	}

	@Override
	public JsonNode toJson(Long value) {
		return value != null ? new LongNode(value) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		if (!indexed) {
			schema.put("index", "no");
		}
	}
}
