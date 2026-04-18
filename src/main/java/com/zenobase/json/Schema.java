package com.zenobase.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import org.jspecify.annotations.Nullable;

import com.zenobase.search.constraints.ConstraintBuilder;

public class Schema {

	private final String typeName;
	private final ObjectNode schema;
	private final ImmutableMap<String, Field<?>> fields;
	private final ImmutableMultimap<String, ConstraintBuilder> constraintBuilders;

	public Schema(
		String typeName,
		ObjectNode schema,
		ImmutableMap<String, Field<?>> fields,
		ImmutableMultimap<String, ConstraintBuilder> constraintBuilders
	) {
		this.typeName = typeName;
		this.schema = schema;
		this.fields = fields;
		this.constraintBuilders = constraintBuilders;
	}

	public String getTypeName() {
		return typeName;
	}

	public @Nullable Field<?> getField(String path) {
		return fields.get(path);
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
