package com.zenobase.json;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import com.zenobase.common.DateTimeFormat;
import com.zenobase.search.DateTimeConstraintBuilder;
import com.zenobase.search.DateTimeRangeConstraintBuilder;

public class DateTimeField extends Field<DateTime> {

	private final ShadowDateTimeField shadow;

	public DateTimeField(String name) {
		super(name, DateTime.class, "date");
		shadow = new ShadowDateTimeField(this);
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
	public void createSchema(ObjectNode schema) {
		super.createSchema(schema);
		shadow.createSchema(schema);
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("format", "date_time");
	}

	@Override
	public void prePersist(ObjectNode node) {
		for (JsonNode childNode : getNodes(node)) {
			shadow.addValue(node, getValue(childNode));
		}
	}

	@Override
	public void postPersist(ObjectNode node) {
		shadow.setValue(node, null);
	}
}
