package com.zenobase.json;

import java.util.List;

import org.joda.time.DateTime;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;

public class DateTimeRangeField extends DateTimeField {

	private final DateTimeField min, max;

	public DateTimeRangeField(String name) {
		super(name);
		min = new DateTimeField(name + "$min");
		max = new DateTimeField(name + "$max");
		min.copyConstraintBuilders(this);
		max.copyConstraintBuilders(this);

	}

	@Override
	public void createSchema(ObjectNode schema) {
		super.createSchema(schema);
		min.createSchema(schema);
		max.createSchema(schema);
	}

	@Override
	public void prePersist(ObjectNode node) {
		super.prePersist(node);
		List<DateTime> values = getValues(node);
		min.setValue(node, Ordering.<DateTime>natural().min(values));
		max.setValue(node, Ordering.<DateTime>natural().max(values));
		min.prePersist(node);
		max.prePersist(node);
	}

	@Override
	public void postPersist(ObjectNode node) {
		super.postPersist(node);
		min.setValue(node, null);
		max.setValue(node, null);
		min.postPersist(node);
		max.postPersist(node);
	}
}
