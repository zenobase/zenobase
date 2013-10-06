package com.zenobase.json;

import java.util.Map;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.search.ConstraintBuilder;
import com.zenobase.search.ShadowFieldConstraintBuilder;

public class ShadowDateTimeField extends Field<DateTime> {

	private static final IntegerField YEAR = new IntegerField("year");
	private static final IntegerField MONTH_OF_YEAR = new IntegerField("month_of_year");
	private static final IntegerField DAY_OF_YEAR = new IntegerField("day_of_year");
	private static final IntegerField DAY_OF_MONTH = new IntegerField("day_of_month");
	private static final IntegerField DAY_OF_WEEK = new IntegerField("day_of_week");
	private static final IntegerField HOUR_OF_DAY = new IntegerField("hour_of_day");
	private static final IntegerField OFFSET = new IntegerField("offset");

	private final DateTimeField parent;

	public ShadowDateTimeField(DateTimeField parent) {
		super("$" + parent.getName(), DateTime.class, "object");
		this.parent = parent;
		addConstraintBuilders(YEAR);
		addConstraintBuilders(MONTH_OF_YEAR);
		addConstraintBuilders(DAY_OF_YEAR);
		addConstraintBuilders(DAY_OF_MONTH);
		addConstraintBuilders(DAY_OF_WEEK);
		addConstraintBuilders(HOUR_OF_DAY);
		addConstraintBuilders(OFFSET);
	}

	@Override
	protected DateTime getValue(JsonNode node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JsonNode toJson(DateTime value) {
		if (value == null) {
			return NullNode.getInstance();
		}
		ObjectNode node = Nodes.newObject();
		YEAR.setValue(node, value.getYear());
		MONTH_OF_YEAR.setValue(node, value.getMonthOfYear());
		DAY_OF_YEAR.setValue(node, value.getDayOfYear());
		DAY_OF_MONTH.setValue(node, value.getDayOfMonth());
		DAY_OF_WEEK.setValue(node, value.getDayOfWeek());
		HOUR_OF_DAY.setValue(node, value.getHourOfDay());
		OFFSET.setValue(node, value.getZone().getOffset(value) / (60 * 1000));
		return node;
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, YEAR);
		configureSchema(properties, MONTH_OF_YEAR);
		configureSchema(properties, DAY_OF_YEAR);
		configureSchema(properties, DAY_OF_MONTH);
		configureSchema(properties, DAY_OF_WEEK);
		configureSchema(properties, HOUR_OF_DAY);
		configureSchema(properties, OFFSET);
	}

	@Override
	protected void addConstraintBuilders(Field<?> nested) {
		for (Map.Entry<String, ConstraintBuilder> entry : nested.getConstraintBuilders().entries()) {
			parent.addConstraintBuilder(parent.getName() + "." + entry.getKey(), new ShadowFieldConstraintBuilder(entry.getValue()));
		}
	}
}
