package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.LongNode;
import org.codehaus.jackson.node.NullNode;
import org.joda.time.Duration;

import com.zenobase.search.DurationConstraint;
import com.zenobase.search.DurationRangeConstraint;

public class DurationField extends Field<Duration> {

	public DurationField(String name) {
		super(name, Duration.class, "long");
		addConstraint(new DurationRangeConstraint());
		addConstraint(new DurationConstraint());
	}

	@Override
	protected Duration getValue(JsonNode node) {
		return Duration.millis(node.getIntValue());
	}

	@Override
	public JsonNode toJson(Duration value) {
		return value != null ? new LongNode(value.getMillis()) : NullNode.getInstance();
	}
}
