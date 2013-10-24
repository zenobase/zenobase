package com.zenobase.json;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.search.LocalDateTimeConstraintBuilder;
import com.zenobase.search.LocalDateTimeRangeConstraintBuilder;

public class LocalDateTimeField extends Field<LocalDateTime> {

	private final OffsetDateTimeField timeField = new OffsetDateTimeField(this, "time");
	private final IntegerField monthOfYearField = new IntegerField(this, "month_of_year");
	private final IntegerField dayOfYearField = new IntegerField(this, "day_of_year");
	private final IntegerField dayOfMonthField = new IntegerField(this, "day_of_month");
	private final IntegerField dayOfWeekField = new IntegerField(this, "day_of_week");
	private final IntegerField hourOfDayField = new IntegerField(this, "hour_of_day");

	public LocalDateTimeField(String name) {
		super(internal(name), LocalDateTime.class, "object");
		addConstraintBuilder(name, new LocalDateTimeRangeConstraintBuilder(timeField));
		addConstraintBuilder(name, new LocalDateTimeConstraintBuilder(timeField));
		addConstraintBuilders(name, monthOfYearField);
		addConstraintBuilders(name, dayOfYearField);
		addConstraintBuilders(name, dayOfMonthField);
		addConstraintBuilders(name, dayOfWeekField);
		addConstraintBuilders(name, hourOfDayField);
	}

	@Override
	protected LocalDateTime getValue(JsonNode node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JsonNode toJson(LocalDateTime value) {
		if (value == null) {
			return NullNode.getInstance();
		}
		ObjectNode node = Nodes.newObject();
		timeField.setValue(node, value.toDateTime(DateTimeZone.UTC));
		monthOfYearField.setValue(node, value.getMonthOfYear());
		dayOfYearField.setValue(node, value.getDayOfYear());
		dayOfMonthField.setValue(node, value.getDayOfMonth());
		dayOfWeekField.setValue(node, value.getDayOfWeek());
		hourOfDayField.setValue(node, value.getHourOfDay());
		return node;
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, timeField);
		configureSchema(properties, monthOfYearField);
		configureSchema(properties, dayOfYearField);
		configureSchema(properties, dayOfMonthField);
		configureSchema(properties, dayOfWeekField);
		configureSchema(properties, hourOfDayField);
	}

	public static String getLocalTimePath(String parent) {
		return getLocalTimePath(parent, "time");
	}

	public static String getLocalTimePath(String parent, String field) {
		return concat(internal(parent), field);
	}
}
