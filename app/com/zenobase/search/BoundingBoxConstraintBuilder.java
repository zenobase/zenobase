package com.zenobase.search;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.models.Location;

public class BoundingBoxConstraintBuilder implements ConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		String[] tokens = value.split(",");
		return tokens.length == 4
			? build(field, new Location(tokens[2], tokens[1]), new Location(tokens[0], tokens[3]))
			: null;
	}

	private static QueryBuilder build(String field, Location topLeft, Location bottomRight) {
		FilterBuilder filter = FilterBuilders.geoBoundingBoxFilter(field).type("indexed")
			.topLeft(topLeft.getLatitude().doubleValue(), topLeft.getLongitude().doubleValue())
			.bottomRight(bottomRight.getLatitude().doubleValue(), bottomRight.getLongitude().doubleValue());
		return QueryBuilders.constantScoreQuery(filter);
	}
}
