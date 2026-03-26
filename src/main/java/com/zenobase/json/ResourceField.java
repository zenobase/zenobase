package com.zenobase.json;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Resource;
import com.zenobase.search.ExistsConstraintBuilder;

public class ResourceField extends Field<Resource> {

	private final Field<String> titleField;
	private final Field<String> urlField;

	public ResourceField(String name) {
		super(name, Resource.class, "object");
		titleField = new TextField(concat(name, "title"), "title");
		urlField = new TokenField(concat(name, "url"), "url", true);
		addConstraintBuilder(name, new ExistsConstraintBuilder(getPath()));
		addAll(titleField.getConstraintBuilders());
		addAll(urlField.getConstraintBuilders());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, titleField);
		configureSchema(properties, urlField);
	}

	@Override
	protected Resource getValue(JsonNode node) {
		return new Resource(
				Objects.requireNonNull(titleField.getValue((ObjectNode) node)),
				Objects.requireNonNull(urlField.getValue((ObjectNode) node)));
	}

	@Override
	public JsonNode toJson(@Nullable Resource value) {
		return value != null ? toJson(value.title(), value.url()) : NullNode.getInstance();
	}

	private JsonNode toJson(String title, String url) {
		ObjectNode node = Nodes.newObject();
		titleField.setValue(node, title);
		urlField.setValue(node, url);
		return node;
	}
}
