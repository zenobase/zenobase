package com.zenobase.search;

import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import com.zenobase.common.Characters;

public class EpochDateTimeConstraintBuilder extends ConstraintBuilder {

	public EpochDateTimeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public @Nullable Query build(String value) {
		return Characters.isDigits(value) && value.length() > 4
				? Query.of(q -> q.match(m -> m.field(getPath()).query(FieldValue.of(Long.parseLong(value)))))
				: null;
	}
}
