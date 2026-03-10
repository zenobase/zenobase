package com.zenobase.search;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.joda.time.Interval;

import com.zenobase.common.OffsetDateTimeFormat;
import com.zenobase.common.OffsetIntervals;

public class OffsetDateTimeConstraintBuilder extends ConstraintBuilder {

	public OffsetDateTimeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		return OffsetDateTimeFormat.hasOffset(value) ? build(OffsetIntervals.valueOf(value)) : null;
	}

	private Query build(Interval interval) {
		if (interval == null) {
			return null;
		} else if (interval.toDurationMillis() > 1L) {
			String gte = interval.getStart().toString();
			String lt = interval.getEnd().toString();
			return RangeQuery.of(r -> r.field(getPath())
				.gte(JsonData.of(gte))
				.lt(JsonData.of(lt)))._toQuery();
		} else {
			String val = interval.getStart().toString();
			return TermQuery.of(t -> t.field(getPath()).value(FieldValue.of(val)))._toQuery();
		}
	}
}
