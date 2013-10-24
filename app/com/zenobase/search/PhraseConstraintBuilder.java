package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class PhraseConstraintBuilder extends ConstraintBuilder {

	public PhraseConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return isPhrase(value) ? QueryBuilders.matchPhraseQuery(getPath(), value) : null;
	}

	private boolean isPhrase(String value) {
		return value.length() > 2 && value.startsWith("\"") && value.endsWith("\"");
	}
}
