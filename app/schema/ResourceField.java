package schema;

import models.Resource;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.Iterables;
import common.Nodes;

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

	private static void configureSchema(ObjectNode properties, Field<?> field) {
		field.configureSchema(properties.putObject(field.getName()));
	}

	@Override
	protected Resource getValue(JsonNode node) {
		return new Resource(get((ObjectNode) node, TITLE), get((ObjectNode) node, URL));
	}

	private static <T> T get(JsonNode node, Field<T> field) {
		return Iterables.getOnlyElement(field.getValues((ObjectNode) node));
	}

	@Override
	protected JsonNode toJson(Resource value) {
		ObjectNode object = Nodes.newObject();
		TITLE.setValue(object, value.getTitle());
		URL.setValue(object, value.getUrl());
		return object;
	}
}
