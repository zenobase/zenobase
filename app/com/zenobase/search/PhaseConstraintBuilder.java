package com.zenobase.search;

import java.math.BigDecimal;

import org.elasticsearch.index.query.QueryBuilder;

public class PhaseConstraintBuilder extends TermConstraintBuilder {

	public PhaseConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return super.build(new BigDecimal(value).remainder(BigDecimal.ONE).toString());
	}
}
