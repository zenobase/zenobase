package com.zenobase.json;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.collect.ImmutableMultimap;

import com.zenobase.search.ConstraintBuilder;

public class Schema {

	private final String typeName;
	private final ObjectNode schema;
	private final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders;

	public Schema(String typeName, ObjectNode schema, ImmutableMultimap<String, ConstraintBuilder> constraintBuilders) {
		this.typeName = typeName;
		this.schema = schema;
		this.constraintBuilders = constraintBuilders;
	}

	public String getTypeName() {
		return typeName;
	}

	public ImmutableMultimap<String, ConstraintBuilder> getConstraintBuilders() {
		return constraintBuilders;
	}

	@Override
	public String toString() {
		return typeName;
	}

	public ObjectNode toJson() {
		return schema;
	}
}
