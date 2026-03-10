package com.zenobase.search.geo;

import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;

public class GeoCluster {

	private final long count;
	private final String geohash;
	private final Point center;

	public GeoCluster(long count, String geohash, Point center) {
		this.count = count;
		this.geohash = geohash;
		this.center = center;
	}

	public GeoCluster merge(GeoCluster that) {
		long count = this.count + that.count();
		Point center = mean(this.center, count - that.count(), that.center(), that.count());
		return new GeoCluster(count, geohash, center);
	}

	private static Point mean(Point left, long leftWeight, Point right, long rightWeight) {
		double lat = (left.getY() * leftWeight + right.getY() * rightWeight) / (leftWeight + rightWeight);
		double lon = (left.getX() * leftWeight + right.getX() * rightWeight) / (leftWeight + rightWeight);
		return SpatialContext.GEO.getShapeFactory().pointXY(lon, lat);
	}

	public long count() {
		return count;
	}

	public String geohash() {
		return geohash;
	}

	public Point center() {
		return center;
	}

	public GeoBoundingBox bounds() {
		Rectangle bounds = GeohashUtils.decodeBoundary(geohash, SpatialContext.GEO);
		Point topLeft = SpatialContext.GEO.getShapeFactory().pointXY(bounds.getMinX(), bounds.getMaxY());
		Point bottomRight = SpatialContext.GEO.getShapeFactory().pointXY(bounds.getMaxX(), bounds.getMinY());
		return new GeoBoundingBox(topLeft, bottomRight);
	}
}
