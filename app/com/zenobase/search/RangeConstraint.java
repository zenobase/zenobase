package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import com.google.common.base.Preconditions;

public class RangeConstraint implements Constraint {

	@Override
	public QueryBuilder build(String field, String value) {
		String[] tokens = value.split(",", 2);
		Preconditions.checkArgument(tokens.length == 2);
		RangeQueryBuilder query = QueryBuilders.rangeQuery(field);
		if (!tokens[0].isEmpty()) {
			query = query.gte(Integer.parseInt(tokens[0]));
		}
		if (!tokens[1].isEmpty()) {
			query = query.lt(Integer.parseInt(tokens[1]));
		}
		return query;
	}
}
