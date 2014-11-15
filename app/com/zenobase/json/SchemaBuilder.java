package com.zenobase.json;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import com.zenobase.search.ConstraintBuilder;
import com.zenobase.search.OffsetDateTimeConstraintBuilder;
import com.zenobase.search.OffsetDateTimeRangeConstraintBuilder;

public class SchemaBuilder {

	private static final String PROPERTIES = "properties";

	private final String typeName;
	private final ObjectNode schema = Nodes.newObject();
	private final ObjectNode type;
	private final ObjectNode properties;
	private final ImmutableMap.Builder<String, Field<?>> fields = ImmutableMap.<String, Field<?>>builder();
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
		configureTimestampField();
	}

	private void configureSourceField() {
		ObjectNode sourceNode = type.putObject("_source");
		ArrayNode excludesNode = sourceNode.putArray("excludes");
		excludesNode.add("_*");
		excludesNode.add("*._*");
		excludesNode.add("*$*");
		excludesNode.add(DomainNode.VERSION.getName());
	}

	private void configureTypeField() {
		ObjectNode node = type.putObject("_type");
		node.put("index", "no");
	}

	private void configureAllField() {
		ObjectNode node = type.putObject("_all");
		node.put("enabled", false);
	}

	private void configureTimestampField() {
		String fieldName = "_timestamp";
		ObjectNode timestampNode = type.putObject(fieldName);
		timestampNode.put("enabled", true);
		constraintBuilders.put(fieldName, new OffsetDateTimeRangeConstraintBuilder(fieldName));
		constraintBuilders.put(fieldName, new OffsetDateTimeConstraintBuilder(fieldName));
	}

	public SchemaBuilder add(Field<?> field) {
		fields.put(field.getPath(), field);
		field.createSchema(properties);
		constraintBuilders.putAll(field.getConstraintBuilders());
		return this;
	}

	public void setRouting(String path) {
		ObjectNode routingNode = type.putObject("_routing");
		routingNode.put("required", true);
		routingNode.put("path", path);
	}

	public Schema build() {
		return new Schema(typeName, schema.deepCopy(), fields.build(), constraintBuilders.build());
	}
}
