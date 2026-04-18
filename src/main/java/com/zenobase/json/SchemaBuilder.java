package com.zenobase.json;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.zenobase.search.constraints.ConstraintBuilder;

public class SchemaBuilder {

	private static final String PROPERTIES = "properties";

	private final String typeName;
	private final ObjectNode schema = Nodes.newObject();
	private final ObjectNode properties;
	private final ImmutableMap.Builder<String, Field<?>> fields = ImmutableMap.builder();
	private final ImmutableMultimap.Builder<String, ConstraintBuilder> constraintBuilders = ImmutableMultimap.builder();

	public SchemaBuilder(String typeName) {
		this.typeName = typeName;
		this.schema.put("dynamic", "strict");
		this.properties = schema.putObject(PROPERTIES);
		configureSourceField();
	}

	private void configureSourceField() {
		ObjectNode sourceNode = schema.putObject("_source");
		ArrayNode excludesNode = sourceNode.putArray("excludes");
		excludesNode.add("_*");
		excludesNode.add("*._*");
		excludesNode.add("*$*");
		excludesNode.add(DomainNode.VERSION.getName());
		excludesNode.add(DomainNode.SEQ_NO_FIELD);
		excludesNode.add(DomainNode.PRIMARY_TERM_FIELD);
	}

	public SchemaBuilder add(Field<?> field) {
		fields.put(field.getPath(), field);
		field.createSchema(properties);
		constraintBuilders.putAll(field.getConstraintBuilders());
		return this;
	}

	public Schema build() {
		return new Schema(typeName, schema.deepCopy(), fields.build(), constraintBuilders.build());
	}
}
