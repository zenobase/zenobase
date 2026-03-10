package com.zenobase.search;

import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import com.zenobase.models.Location;

public class BoundingBoxConstraintBuilder extends ConstraintBuilder {

	public BoundingBoxConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		String[] tokens = value.split(",");
		return tokens.length == 4 ? build(new Location(tokens[2], tokens[1]), new Location(tokens[0], tokens[3])) : null;
	}

	private QueryBuilder build(Location topLeft, Location bottomRight) {
		return QueryBuilders.geoBoundingBoxQuery(getPath()).setCorners(
			topLeft.getLatitude().doubleValue(), topLeft.getLongitude().doubleValue(),
			bottomRight.getLatitude().doubleValue(), bottomRight.getLongitude().doubleValue());
	}
}
