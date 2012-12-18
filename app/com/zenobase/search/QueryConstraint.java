package com.zenobase.search;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class QueryConstraint {

	private final String field;
	private final String value;

	private QueryConstraint(String field, String value) {
		Preconditions.checkArgument(!field.isEmpty());
		Preconditions.checkArgument(!value.isEmpty());
		this.field = field;
		this.value = value;
	}

	public String getField() {
		return field;
	}

	public String getValue() {
		return value;
	}

	public static QueryConstraint parse(String value) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(value));
		String[] tokens = value.split(":", 2);
		Preconditions.checkArgument(tokens.length == 2);
		return new QueryConstraint(tokens[0], tokens[1]);
	}
}
