package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.zenobase.search.constraints.TermConstraintBuilder;
import com.zenobase.search.constraints.WildcardConstraintBuilder;
import org.jspecify.annotations.Nullable;

public class TokenField extends Field<String> {

	private final boolean indexed;

	public TokenField(String name) {
		this(name, true);
	}

	public TokenField(String name, boolean indexed) {
		this(name, name, indexed);
	}

	public TokenField(String path, String name, boolean indexed) {
		super(path, name, String.class, "keyword");
		this.indexed = indexed;
		if (indexed) {
			addConstraintBuilder(path, new WildcardConstraintBuilder(path));
			addConstraintBuilder(path, new TermConstraintBuilder(path));
		}
	}

	@Override
	protected String getValue(JsonNode node) {
		return node.textValue();
	}

	@Override
	public JsonNode toJson(@Nullable String value) {
		return value != null ? new TextNode(value) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		if (!indexed) {
			schema.put("index", false);
		}
	}
}
