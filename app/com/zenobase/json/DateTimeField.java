package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.joda.time.DateTime;

import com.zenobase.common.DateTimeFormat;
import com.zenobase.search.DateTimeConstraintBuilder;
import com.zenobase.search.DateTimeRangeConstraintBuilder;

public class DateTimeField extends Field<DateTime> {

	public DateTimeField(String name) {
		super(name, DateTime.class, "date");
		addConstraint(new DateTimeRangeConstraintBuilder());
		addConstraint(new DateTimeConstraintBuilder());
	}

	@Override
	protected DateTime getValue(JsonNode node) {
		return DateTimeFormat.parse(node.getTextValue());
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
