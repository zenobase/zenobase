package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateTimeField extends Field<DateTime> {

	private final DateTimeFormatter formatter = ISODateTimeFormat.dateTime().withOffsetParsed();

	public DateTimeField(String name) {
		super(name, DateTime.class, "date");
	}

	@Override
	protected DateTime getValue(JsonNode node) {
		return formatter.parseDateTime(node.getTextValue());
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
