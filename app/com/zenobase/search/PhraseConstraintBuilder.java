package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class PhraseConstraintBuilder implements ConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		return isPhrase(value) ? QueryBuilders.matchPhraseQuery(field, value) : null;
	}

	private boolean isPhrase(String value) {
		return value.length() > 2 && value.startsWith("\"") && value.endsWith("\"");
	}
}
