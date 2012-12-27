package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;

public interface ConstraintBuilder {

	QueryBuilder build(String field, String value);
}
