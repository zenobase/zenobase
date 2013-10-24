package com.zenobase.json;

import org.joda.time.Duration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;

import com.zenobase.search.DurationConstraintBuilder;
import com.zenobase.search.DurationRangeConstraintBuilder;

public class DurationField extends Field<Duration> {

	public DurationField(String name) {
		super(name, Duration.class, "long");
		addConstraintBuilder(name, new DurationRangeConstraintBuilder(this));
		addConstraintBuilder(name, new DurationConstraintBuilder(this));
	}

	@Override
	protected Duration getValue(JsonNode node) {
		return Duration.millis(node.intValue());
	}

	@Override
	public JsonNode toJson(Duration value) {
		return value != null ? new LongNode(value.getMillis()) : NullNode.getInstance();
	}
}
