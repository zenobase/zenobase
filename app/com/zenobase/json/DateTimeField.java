package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.joda.time.DateTime;

import com.zenobase.common.DateTimeFormat;
import com.zenobase.search.DateTimeConstraintBuilder;
import com.zenobase.search.DateTimeRangeConstraintBuilder;

public class DateTimeField extends Field<DateTime> {

	public DateTimeField(String name) {
		super(name, DateTime.class, "date");
		addConstraintBuilder(new DateTimeRangeConstraintBuilder());
		addConstraintBuilder(new DateTimeConstraintBuilder());
	}

	@Override
	protected DateTime getValue(JsonNode node) {
		return DateTimeFormat.parse(node.textValue());
	}

	@Override
	public JsonNode toJson(DateTime value) {
		return value != null ? new TextNode(value.toString()) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("format", "date_time");
	}
}
