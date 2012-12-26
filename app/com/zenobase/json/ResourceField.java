package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.models.Resource;

public class ResourceField extends Field<Resource> {

	private static final TextField TITLE = new TextField("title");
	private static final TokenField URL = new TokenField("url");

	public ResourceField(String name) {
		super(name, Resource.class, "object");
		addConstraints(TITLE);
		addConstraints(URL);
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
	public JsonNode toJson(Resource value) {
		return value != null
			? toJson(value.getTitle(), value.getUrl())
			: NullNode.getInstance();
	}

	private static JsonNode toJson(String title, String url) {
		ObjectNode node = Nodes.newObject();
		TITLE.setValue(node, title);
		URL.setValue(node, url);
		return node;
	}
}
