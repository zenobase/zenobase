package com.zenobase.search;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;

public class TermConstraintBuilder extends ConstraintBuilder {

	public TermConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		return Query.of(q -> q.term(t -> t.field(getPath()).value(FieldValue.of(value))));
	}
}
