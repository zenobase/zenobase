package com.zenobase.search;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import com.zenobase.common.Characters;

public class EpochDateTimeConstraintBuilder extends ConstraintBuilder {

	public EpochDateTimeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		return Characters.isDigits(value) && value.length() > 4
			? MatchQuery.of(m -> m.field(getPath()).query(FieldValue.of(Long.parseLong(value))))._toQuery()
			: null;
	}
}
