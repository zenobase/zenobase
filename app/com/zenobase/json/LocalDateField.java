package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.joda.time.LocalDate;

public class LocalDateField extends Field<LocalDate> {

	public LocalDateField(String name) {
		super(name, LocalDate.class, "date");
	}

	@Override
	protected LocalDate getValue(JsonNode node) {
		return LocalDate.parse(node.textValue());
	}

	@Override
	public JsonNode toJson(LocalDate value) {
		return value != null ? new TextNode(value.toString()) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("format", "basic_date");
	}
}
