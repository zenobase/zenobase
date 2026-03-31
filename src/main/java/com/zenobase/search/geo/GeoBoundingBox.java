package com.zenobase.search.geo;

import com.google.common.base.Preconditions;
import org.locationtech.spatial4j.shape.Point;

public record GeoBoundingBox(Point topLeft, Point bottomRight) {

	public GeoBoundingBox {
		Preconditions.checkArgument(topLeft.getY() >= bottomRight.getY());
		Preconditions.checkArgument(topLeft.getX() <= bottomRight.getX());
	}
}
