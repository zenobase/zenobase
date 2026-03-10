package com.zenobase.search.geo;

import com.google.common.base.Preconditions;
import org.locationtech.spatial4j.shape.Point;

public class GeoBoundingBox {

	private final Point topLeft, bottomRight;

	public GeoBoundingBox(Point topLeft, Point bottomRight) {
		Preconditions.checkArgument(topLeft.getY() >= bottomRight.getY());
		Preconditions.checkArgument(topLeft.getX() <= bottomRight.getX());
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}

	public Point topLeft() {
		return topLeft;
	}

	public Point bottomRight() {
		return bottomRight;
	}
}
