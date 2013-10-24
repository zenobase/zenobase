package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.json.Field;

public class PhraseConstraintBuilder extends ConstraintBuilder {

	public PhraseConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	public QueryBuilder build(String value) {
		return isPhrase(value) ? QueryBuilders.matchPhraseQuery(getField().getPath(), value) : null;
	}

	private boolean isPhrase(String value) {
		return value.length() > 2 && value.startsWith("\"") && value.endsWith("\"");
	}
}
