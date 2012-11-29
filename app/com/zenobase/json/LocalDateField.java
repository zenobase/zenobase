package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.joda.time.LocalDate;

public class LocalDateField extends Field<LocalDate> {

	public LocalDateField(String name) {
		super(name, LocalDate.class, "date");
	}

	@Override
	protected LocalDate getValue(JsonNode node) {
		return LocalDate.parse(node.getTextValue());
	}

	@Override
	protected JsonNode toJson(LocalDate value) {
		return new TextNode(value.toString());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("format", "basic_date");
	}
}
