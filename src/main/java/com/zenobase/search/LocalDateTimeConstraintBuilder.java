package com.zenobase.search;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import com.zenobase.common.LocalInterval;
import com.zenobase.common.LocalIntervals;

public class LocalDateTimeConstraintBuilder extends ConstraintBuilder {

	public LocalDateTimeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		return build(LocalIntervals.valueOf(value));
	}

	private Query build(LocalInterval interval) {
		if (interval == null) {
			return null;
		} else if (interval.toDurationMillis() > 1L) {
			String gte = toString(interval.getStart());
			String lt = toString(interval.getEnd());
			return Query.of(q -> q.range(r -> r.field(getPath())
				.gte(JsonData.of(gte))
				.lt(JsonData.of(lt))));
		} else {
			String val = toString(interval.getStart());
			return Query.of(q -> q.term(t -> t.field(getPath()).value(FieldValue.of(val))));
		}
	}

	private static String toString(LocalDateTime value) {
		return value.toDateTime(DateTimeZone.UTC).toString();
	}
}
