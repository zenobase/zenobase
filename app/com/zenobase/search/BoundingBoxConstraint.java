package com.zenobase.search;

import org.elasticsearch.index.mapper.geo.GeoPoint;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class BoundingBoxConstraint implements Constraint {

	@Override
	public QueryBuilder build(String field, String value) {
		String[] c = value.split(",");
		GeoPoint topLeft = new GeoPoint(Double.parseDouble(c[2]), Double.parseDouble(c[1]));
		GeoPoint bottomRight = new GeoPoint(Double.parseDouble(c[0]), Double.parseDouble(c[3]));
		FilterBuilder locationFilter = FilterBuilders.geoBoundingBoxFilter(field)
			.topLeft(topLeft.getLat(), topLeft.getLon())
			.bottomRight(bottomRight.getLat(), bottomRight.getLon())
			.type("indexed");
		return QueryBuilders.constantScoreQuery(locationFilter);
	}
}
