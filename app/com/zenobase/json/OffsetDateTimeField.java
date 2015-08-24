package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.joda.time.DateTime;

import com.zenobase.common.OffsetDateTimeFormat;
import com.zenobase.search.EpochDateTimeConstraintBuilder;
import com.zenobase.search.EpochDateTimeRangeConstraintBuilder;
import com.zenobase.search.OffsetDateTimeConstraintBuilder;
import com.zenobase.search.OffsetDateTimeRangeConstraintBuilder;
import com.zenobase.search.PeriodRangeConstraintBuilder;

public class OffsetDateTimeField extends Field<DateTime> {

	public OffsetDateTimeField(String name) {
		super(name, DateTime.class, "date");
		addConstraintBuilder(name, new PeriodRangeConstraintBuilder(getPath()));
		addConstraintBuilder(name, new EpochDateTimeRangeConstraintBuilder(getPath()));
		addConstraintBuilder(name, new OffsetDateTimeRangeConstraintBuilder(getPath()));
		addConstraintBuilder(name, new EpochDateTimeConstraintBuilder(getPath()));
		addConstraintBuilder(name, new OffsetDateTimeConstraintBuilder(getPath()));
	}

	@Override
	protected DateTime getValue(JsonNode node) {
		return OffsetDateTimeFormat.parse(node.textValue());
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
