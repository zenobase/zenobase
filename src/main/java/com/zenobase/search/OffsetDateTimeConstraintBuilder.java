package com.zenobase.search;

import org.joda.time.Interval;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import com.zenobase.common.OffsetDateTimeFormat;
import com.zenobase.common.OffsetIntervals;

public class OffsetDateTimeConstraintBuilder extends ConstraintBuilder {

	public OffsetDateTimeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public @Nullable Query build(String value) {
		return OffsetDateTimeFormat.hasOffset(value) ? build(OffsetIntervals.valueOf(value)) : null;
	}

	private @Nullable Query build(@Nullable Interval interval) {
		if (interval == null) {
			return null;
		} else if (interval.toDurationMillis() > 1L) {
			String gte = interval.getStart().toString();
			String lt = interval.getEnd().toString();
			return Query.of(
					q -> q.range(r -> r.field(getPath()).gte(JsonData.of(gte)).lt(JsonData.of(lt))));
		} else {
			String val = interval.getStart().toString();
			return Query.of(q -> q.term(t -> t.field(getPath()).value(FieldValue.of(val))));
		}
	}
}
