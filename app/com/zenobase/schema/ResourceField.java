package com.zenobase.schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.common.Nodes;
import com.zenobase.models.Resource;

public class ResourceField extends Field<Resource> {

	private static final TextField TITLE = new TextField("title");
	private static final TokenField URL = new TokenField("url");

	public ResourceField(String name) {
		super(name, Resource.class, "object");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, TITLE);
		configureSchema(properties, URL);
	}

	@Override
	protected Resource getValue(JsonNode node) {
		return new Resource(TITLE.getValue((ObjectNode) node), URL.getValue((ObjectNode) node));
	}

	@Override
	protected JsonNode toJson(Resource value) {
		ObjectNode node = Nodes.newObject();
		TITLE.setValue(node, value.getTitle());
		URL.setValue(node, value.getUrl());
		return node;
	}
}
