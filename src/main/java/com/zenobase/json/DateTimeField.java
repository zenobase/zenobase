package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.search.constraints.ExistsConstraintBuilder;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;

public class DateTimeField extends Field<DateTime> {

	private final OffsetDateTimeField offset;
	private final LocalDateTimeField local;

	public DateTimeField(String name) {
		super(name, DateTime.class, "date");
		offset = new OffsetDateTimeField(name);
		offset.copyConstraintBuilders(this);
		local = new LocalDateTimeField(name);
		local.copyConstraintBuilders(this);
		addConstraintBuilder(name, new ExistsConstraintBuilder(getPath()));
	}

	@Override
	protected DateTime getValue(JsonNode node) {
		return offset.getValue(node);
	}

	@Override
	public JsonNode toJson(@Nullable DateTime value) {
		return offset.toJson(value);
	}

	@Override
	public void createSchema(ObjectNode schema) {
		offset.createSchema(schema);
		local.createSchema(schema);
	}

	@Override
	public void prePersist(ObjectNode node) {
		for (JsonNode childNode : getNodes(node)) {
			local.addValue(node, getValue(childNode).toLocalDateTime());
		}
	}

	@Override
	public void postPersist(ObjectNode node) {
		local.setValue(node, null);
	}
}
