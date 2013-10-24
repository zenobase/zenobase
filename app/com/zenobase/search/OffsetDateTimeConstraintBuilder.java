package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.Interval;

import com.zenobase.common.OffsetDateTimeFormat;
import com.zenobase.common.OffsetIntervals;

public class OffsetDateTimeConstraintBuilder extends ConstraintBuilder {

	public OffsetDateTimeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return OffsetDateTimeFormat.hasOffset(value) ? build(OffsetIntervals.valueOf(value)) : null;
	}

	private QueryBuilder build(Interval interval) {
		if (interval == null) {
			return null;
		} else if (interval.toDurationMillis() > 1L) {
			return QueryBuilders.rangeQuery(getPath()).gte(interval.getStart().toString()).lt(interval.getEnd().toString());
		} else {
			return QueryBuilders.termQuery(getPath(), interval.getStart().toString());
		}
	}
}
