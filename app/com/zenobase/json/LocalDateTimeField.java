package com.zenobase.json;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.search.LocalDateTimeConstraintBuilder;
import com.zenobase.search.LocalDateTimeRangeConstraintBuilder;

public class LocalDateTimeField extends Field<LocalDateTime> {

	private final NestedField<DateTime> timeField = nest(new OffsetDateTimeField("time"));
	private final NestedField<Integer> monthOfYearField = nest(new IntegerField("month_of_year", true));
	private final NestedField<Integer> dayOfYearField = nest(new IntegerField("day_of_year", true));
	private final NestedField<Integer> dayOfMonthField = nest(new IntegerField("day_of_month", true));
	private final NestedField<Integer> dayOfWeekField = nest(new IntegerField("day_of_week", true));
	private final NestedField<Integer> hourOfDayField = nest(new IntegerField("hour_of_day", true));

	public LocalDateTimeField(String name) {
		super(internal(name), LocalDateTime.class, "object");
		addConstraintBuilder(name, new LocalDateTimeRangeConstraintBuilder(timeField.getPath()));
		addConstraintBuilder(name, new LocalDateTimeConstraintBuilder(timeField.getPath()));
		monthOfYearField.addConstraintBuilders(name, this);
		dayOfYearField.addConstraintBuilders(name, this);
		dayOfMonthField.addConstraintBuilders(name, this);
		dayOfWeekField.addConstraintBuilders(name, this);
		hourOfDayField.addConstraintBuilders(name, this);
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
