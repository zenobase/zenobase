package com.zenobase.search;

import org.opensearch.client.opensearch._types.query_dsl.MatchPhraseQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public class PhraseConstraintBuilder extends ConstraintBuilder {

	public PhraseConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		return isPhrase(value) ? MatchPhraseQuery.of(m -> m.field(getPath()).query(value))._toQuery() : null;
	}

	private boolean isPhrase(String value) {
		return value.length() > 2 && value.startsWith("\"") && value.endsWith("\"");
	}
}
