package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import com.zenobase.search.PhraseConstraintBuilder;
import com.zenobase.search.TermConstraintBuilder;
import com.zenobase.search.WildcardConstraintBuilder;

public class TextField extends Field<String> {

	public TextField(String name) {
		super(name, String.class, "string");
		addConstraint(new PhraseConstraintBuilder());
		addConstraint(new WildcardConstraintBuilder());
		addConstraint(new TermConstraintBuilder());
	}

	@Override
	protected String getValue(JsonNode node) {
		return node.getTextValue();
	}

	@Override
	public JsonNode toJson(String value) {
		return value != null ? new TextNode(value) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "analyzed");
	}
}
