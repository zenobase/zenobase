package com.zenobase.json;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.search.LocalDateTimeConstraintBuilder;
import com.zenobase.search.LocalDateTimeRangeConstraintBuilder;

public class LocalDateTimeField extends Field<LocalDateTime> {

	public static final OffsetDateTimeField TIME = new OffsetDateTimeField("time");
	private static final IntegerField MONTH_OF_YEAR = new IntegerField("month_of_year");
	private static final IntegerField DAY_OF_YEAR = new IntegerField("day_of_year");
	private static final IntegerField DAY_OF_MONTH = new IntegerField("day_of_month");
	private static final IntegerField DAY_OF_WEEK = new IntegerField("day_of_week");
	private static final IntegerField HOUR_OF_DAY = new IntegerField("hour_of_day");

	public LocalDateTimeField(String name) {
		super(name, LocalDateTime.class, "object");
		addConstraintBuilder(new LocalDateTimeRangeConstraintBuilder(name + "." + TIME.getName()));
		addConstraintBuilder(new LocalDateTimeConstraintBuilder(name + "." + TIME.getName()));
		addConstraintBuilders(MONTH_OF_YEAR);
		addConstraintBuilders(DAY_OF_YEAR);
		addConstraintBuilders(DAY_OF_MONTH);
		addConstraintBuilders(DAY_OF_WEEK);
		addConstraintBuilders(HOUR_OF_DAY);
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
		TIME.setValue(node, value.toDateTime(DateTimeZone.UTC));
		MONTH_OF_YEAR.setValue(node, value.getMonthOfYear());
		DAY_OF_YEAR.setValue(node, value.getDayOfYear());
		DAY_OF_MONTH.setValue(node, value.getDayOfMonth());
		DAY_OF_WEEK.setValue(node, value.getDayOfWeek());
		HOUR_OF_DAY.setValue(node, value.getHourOfDay());
		return node;
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, TIME);
		configureSchema(properties, MONTH_OF_YEAR);
		configureSchema(properties, DAY_OF_YEAR);
		configureSchema(properties, DAY_OF_MONTH);
		configureSchema(properties, DAY_OF_WEEK);
		configureSchema(properties, HOUR_OF_DAY);
	}
}
