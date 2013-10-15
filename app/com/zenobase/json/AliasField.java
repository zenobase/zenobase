package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Alias;

public class AliasField extends Field<Alias> {

	public static final TokenField ID = new TokenField("@id", true);

	public AliasField(String name) {
		super(name, Alias.class, "object");
		addConstraintBuilders(ID);
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, ID);
	}

	@Override
	protected Alias getValue(JsonNode node) {
		return new Alias(ID.getValue((ObjectNode) node));
	}

	@Override
	public JsonNode toJson(Alias value) {
		return value != null
			? toJson(value.getId())
			: NullNode.getInstance();
	}

	private static JsonNode toJson(String id) {
		ObjectNode node = Nodes.newObject();
		ID.setValue(node, id);
		return node;
	}
}
