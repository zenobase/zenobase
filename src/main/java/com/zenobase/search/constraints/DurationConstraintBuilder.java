package com.zenobase.search.constraints;

import org.joda.time.Duration;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import com.zenobase.common.DurationFormat;

public class DurationConstraintBuilder extends ConstraintBuilder {

	public DurationConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		return build(DurationFormat.parse(value));
	}

	private Query build(Duration value) {
		return Query.of(q -> q.term(t -> t.field(getPath()).value(FieldValue.of(value.getMillis()))));
	}
}
