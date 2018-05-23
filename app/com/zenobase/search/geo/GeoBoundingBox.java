package com.zenobase.search.geo;

import com.google.common.base.Preconditions;
import org.elasticsearch.common.geo.GeoPoint;

public class GeoBoundingBox {

	private final GeoPoint topLeft, bottomRight;

	public GeoBoundingBox(GeoPoint topLeft, GeoPoint bottomRight) {
		Preconditions.checkArgument(topLeft.getLat() >= bottomRight.getLat());
		Preconditions.checkArgument(topLeft.getLon() <= bottomRight.getLon());
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}

	public GeoPoint topLeft() {
		return topLeft;
	}

	public GeoPoint bottomRight() {
		return bottomRight;
	}
}
