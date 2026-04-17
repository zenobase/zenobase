package com.zenobase.search.constraints;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public record QueryConstraint(String field, String value) {

	public QueryConstraint {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(field));
		Preconditions.checkArgument(!Strings.isNullOrEmpty(value));
	}

	public static QueryConstraint parse(String value) {
		Preconditions.checkNotNull(value);
		String[] tokens = value.split(":", 2);
		Preconditions.checkArgument(tokens.length == 2);
		return new QueryConstraint(tokens[0], tokens[1]);
	}
}
