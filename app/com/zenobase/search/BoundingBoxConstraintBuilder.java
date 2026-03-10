package com.zenobase.search;

import org.opensearch.client.opensearch._types.GeoBounds;
import org.opensearch.client.opensearch._types.GeoLocation;
import org.opensearch.client.opensearch._types.LatLonGeoLocation;
import org.opensearch.client.opensearch._types.TopLeftBottomRightGeoBounds;
import org.opensearch.client.opensearch._types.query_dsl.GeoBoundingBoxQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import com.zenobase.models.Location;

public class BoundingBoxConstraintBuilder extends ConstraintBuilder {

	public BoundingBoxConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		String[] tokens = value.split(",");
		return tokens.length == 4 ? build(new Location(tokens[2], tokens[1]), new Location(tokens[0], tokens[3])) : null;
	}

	private Query build(Location topLeft, Location bottomRight) {
		double tlLat = topLeft.getLatitude().doubleValue();
		double tlLon = topLeft.getLongitude().doubleValue();
		double brLat = bottomRight.getLatitude().doubleValue();
		double brLon = bottomRight.getLongitude().doubleValue();
		return GeoBoundingBoxQuery.of(g -> g
			.field(getPath())
			.boundingBox(GeoBounds.of(gb -> gb.tlbr(TopLeftBottomRightGeoBounds.of(tlbr -> tlbr
				.topLeft(GeoLocation.of(gl -> gl.latlon(LatLonGeoLocation.of(ll -> ll.lat(tlLat).lon(tlLon)))))
				.bottomRight(GeoLocation.of(gl -> gl.latlon(LatLonGeoLocation.of(ll -> ll.lat(brLat).lon(brLon)))))
			))))
		)._toQuery();
	}
}
