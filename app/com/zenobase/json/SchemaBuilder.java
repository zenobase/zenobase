package com.zenobase.json;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.collect.ImmutableMultimap;

import com.zenobase.search.ConstraintBuilder;


public class SchemaBuilder {

	private static final String PROPERTIES = "properties";

	private final String typeName;
	private final ObjectNode schema = Nodes.newObject();
	private final ObjectNode type;
	private final ObjectNode properties;
	private final ImmutableMultimap.Builder<String, ConstraintBuilder> constraintBuilders =
		ImmutableMultimap.<String, ConstraintBuilder>builder();

	public SchemaBuilder(String typeName) {
		this.typeName = typeName;
		this.type = schema.putObject(typeName);
		this.type.put("dynamic", "strict");
		this.properties = type.putObject(PROPERTIES);
		configureSourceField();
		configureTypeField();
		configureAllField();
	}

	private void configureSourceField() {
		ObjectNode sourceNode = type.putObject("_source");
		ArrayNode excludesNode = sourceNode.putArray("excludes");
		excludesNode.add("_*");
		excludesNode.add("*._*");
		excludesNode.add(DomainNode.VERSION.getName());
	}

	private void configureTypeField() {
		ObjectNode sourceNode = type.putObject("_type");
		sourceNode.put("index", "no");
	}

	private void configureAllField() {
		ObjectNode sourceNode = type.putObject("_all");
		sourceNode.put("enabled", false);
	}

	public SchemaBuilder add(Field<?> field) {
		field.configureSchema(properties.putObject(field.getName()));
		constraintBuilders.putAll(field.getConstraintBuilders());
		return this;
	}

	public Schema build() {
		return new Schema(typeName, Nodes.copy(schema), constraintBuilders.build());
	}
}
