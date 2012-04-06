package schema;

import models.Resource;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.Iterables;
import common.Nodes;

public class ResourceType extends Type<Resource> {

	private static final Field<String> title = Field.of("title", new TextType());
	private static final Field<String> url = Field.of("url", new TokenType());

	public ResourceType() {
		super(Resource.class, "object");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, title);
		configureSchema(properties, url);
	}

	private static void configureSchema(ObjectNode properties, Field<?> field) {
		field.getType().configureSchema(properties.putObject(field.getName()));
	}

	@Override
	protected Resource get(JsonNode node) {
		return get((ObjectNode) node);
	}

	private static Resource get(ObjectNode node) {
		return new Resource(get((ObjectNode) node, title), get((ObjectNode) node, url));
	}

	private static <T> T get(JsonNode node, Field<T> field) {
		return Iterables.getOnlyElement(field.getType().get((ObjectNode) node, field.getName()));
	}

	@Override
	protected JsonNode get(Resource value) {
		ObjectNode object = Nodes.newObject();
		add(object, title, value.getTitle());
		add(object, url, value.getUrl());
		return object;
	}

	private static <T> void add(ObjectNode object, Field<T> field, T value) {
		field.getType().add(object, field.getName(), value);
	}
}
