package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Alias;

public class AliasField extends Field<Alias> {

	public static final String ID = "@id";
	public static final String FILTER = "filter";

	private final NestedField<String> idField = nest(new TokenField(ID, true));
	private final NestedField<String> filterField = nest(new TokenField(FILTER, false));

	public AliasField(String name) {
		super(name, Alias.class, "object");
		idField.addConstraintBuilders(name, this);
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, idField);
		configureSchema(properties, filterField);
	}

	@Override
	protected Alias getValue(JsonNode node) {
		ObjectNode object = (ObjectNode) node;
		return new Alias(idField.getValue(object), filterField.getValue(object));
	}

	@Override
	public JsonNode toJson(Alias value) {
		return value != null ? toJson(value.id(), value.filter()) : NullNode.getInstance();
	}

	private JsonNode toJson(String id, String filter) {
		ObjectNode node = Nodes.newObject();
		idField.setValue(node, id);
		filterField.setValue(node, filter);
		return node;
	}
}
