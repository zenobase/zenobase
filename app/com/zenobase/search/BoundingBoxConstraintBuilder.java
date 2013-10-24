package com.zenobase.search;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.json.Field;
import com.zenobase.models.Location;

public class BoundingBoxConstraintBuilder extends ConstraintBuilder {

	public BoundingBoxConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	public QueryBuilder build(String value) {
		String[] tokens = value.split(",");
		return tokens.length == 4 ? build(new Location(tokens[2], tokens[1]), new Location(tokens[0], tokens[3])) : null;
	}

	private QueryBuilder build(Location topLeft, Location bottomRight) {
		FilterBuilder filter = FilterBuilders.geoBoundingBoxFilter(getField().getPath()).type("indexed")
			.topLeft(topLeft.getLatitude().doubleValue(), topLeft.getLongitude().doubleValue())
			.bottomRight(bottomRight.getLatitude().doubleValue(), bottomRight.getLongitude().doubleValue());
		return QueryBuilders.constantScoreQuery(filter);
	}
}
